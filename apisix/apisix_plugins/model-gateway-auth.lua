--
-- model-gateway-auth插件。
-- 将业务JWT转换为new-api专属API Key后转发到上游。
--
local core = require("apisix.core")
local jwt = require("resty.jwt")
local http = require("resty.http")
local redis_util = require("apisix.utils.redis")
local cipher = require("resty.openssl.cipher")

local ngx = ngx
local str_sub = string.sub
local str_lower = string.lower
local str_match = string.match
local tonumber = tonumber
local tostring = tostring

local plugin_name = "model-gateway-auth"
local credential_status_enable = 0
local redis_key_prefix = "gateway:newapi:credential:"
local aes_gcm_iv_size = 12
local aes_gcm_tag_size = 16
local build_redis_conf

local schema = {
    type = "object",
    properties = {
        jwt_public_key = {type = "string", minLength = 1},
        redis_url = {type = "string", minLength = 1},
        redis_host = {type = "string", minLength = 1},
        redis_port = {type = "integer", default = 6379, minimum = 1},
        redis_database = {type = "integer", default = 0, minimum = 0},
        redis_password = {type = "string"},
        redis_username = {type = "string"},
        redis_timeout = {type = "integer", default = 1000, minimum = 1},
        redis_ssl = {type = "boolean", default = false},
        redis_ssl_verify = {type = "boolean", default = false},
        redis_keepalive_timeout = {type = "integer", default = 60000, minimum = 1},
        redis_keepalive_pool = {type = "integer", default = 100, minimum = 1},
        credential_ensure_url = {type = "string", minLength = 1},
        gateway_secret = {type = "string", minLength = 1},
        http_timeout = {type = "integer", default = 1500, minimum = 1},
        aes_keys = {
            type = "object",
            minProperties = 1,
            additionalProperties = {type = "string", minLength = 1},
        },
    },
    required = {
        "jwt_public_key",
        "credential_ensure_url",
        "gateway_secret",
        "aes_keys",
    },
    encrypt_fields = {"gateway_secret"},
}

local _M = {
    version = 0.1,
    priority = 2600,
    type = "auth",
    name = plugin_name,
    schema = schema,
}

-- 校验插件配置。
function _M.check_schema(conf)
    local ok, err = core.schema.check(schema, conf)
    if not ok then
        return false, err
    end

    if not conf.redis_url and not conf.redis_host then
        return false, "property redis_url or redis_host is required"
    end

    local redis_conf, redis_conf_err = build_redis_conf(conf)
    if not redis_conf then
        return false, redis_conf_err
    end

    for key_id, encoded_key in pairs(conf.aes_keys) do
        local aes_key = ngx.decode_base64(encoded_key)
        if not aes_key or #aes_key ~= 32 then
            return false, "aes key " .. key_id .. " must be base64 encoded 32 bytes"
        end
    end

    return true
end

-- 返回统一错误响应。
local function reject(status, message)
    return status, {message = message}
end

-- 提取Bearer JWT。
local function read_bearer_jwt(ctx)
    local authorization = core.request.header(ctx, "Authorization")
    if not authorization or #authorization <= 7 then
        return nil
    end

    if str_lower(str_sub(authorization, 1, 7)) ~= "bearer " then
        return nil
    end

    return str_sub(authorization, 8)
end

-- 验证JWT并读取业务用户。
local function verify_gateway_jwt(conf, token)
    local jwt_obj = jwt:load_jwt(token)
    if not jwt_obj.valid then
        core.log.warn("invalid gateway jwt: ", jwt_obj.reason)
        return nil, "Token无效或已过期"
    end

    if not jwt_obj.header or jwt_obj.header.alg ~= "RS256" then
        core.log.warn("unsupported gateway jwt alg")
        return nil, "Token无效或已过期"
    end

    local jwt_public_key = conf.jwt_public_key:gsub("\\n", "\n")
    jwt_obj = jwt:verify_jwt_obj(jwt_public_key, jwt_obj)
    if not jwt_obj.verified then
        core.log.warn("gateway jwt verify failed: ", jwt_obj.reason)
        return nil, "Token无效或已过期"
    end

    local payload = jwt_obj.payload or {}
    if not payload.eff or tonumber(payload.eff) <= ngx.time() then
        core.log.warn("gateway jwt expired")
        return nil, "Token无效或已过期"
    end

    if not payload.loginId then
        core.log.warn("gateway jwt missing loginId")
        return nil, "Token无效或已过期"
    end

    return {
        user_id = tostring(payload.loginId),
    }
end

-- 查询Redis凭证缓存。
local function read_redis_credential(conf, user_id)
    local red, err = redis_util.new(conf)
    if not red then
        core.log.error("redis connect failed: ", err)
        return nil, "redis_error"
    end

    local credential_json, get_err = red:get(redis_key_prefix .. user_id)
    local ok, keepalive_err = red:set_keepalive(
        conf.redis_keepalive_timeout,
        conf.redis_keepalive_pool
    )
    if not ok then
        core.log.warn("redis set_keepalive failed: ", keepalive_err)
    end

    if get_err then
        core.log.error("redis get credential failed: ", get_err)
        return nil, "redis_error"
    end
    if not credential_json or credential_json == ngx.null then
        return nil, "miss"
    end

    local credential, decode_err = core.json.decode(credential_json)
    if not credential then
        core.log.error("redis credential json decode failed: ", decode_err)
        return nil, "invalid"
    end

    return credential
end

-- 调用用户系统补齐凭证。
local function ensure_credential(conf, ctx, token, user_id)
    local httpc = http.new()
    httpc:set_timeout(conf.http_timeout)

    local body = core.json.encode({
        userId = user_id,
        requestId = ctx.var.request_id,
    })

    local res, err = httpc:request_uri(conf.credential_ensure_url, {
        method = "POST",
        body = body,
        headers = {
            ["Content-Type"] = "application/json",
            ["X-Gateway-Secret"] = conf.gateway_secret,
            ["Authorization"] = "Bearer " .. token,
        },
        keepalive = true,
    })

    if not res then
        core.log.error("credential ensure request failed: ", err)
        return nil, 503, "凭证服务暂不可用"
    end

    if res.status < 200 or res.status >= 300 then
        core.log.warn("credential ensure response status: ", res.status)
        if res.status == 401 or res.status == 403 then
            return nil, res.status, "Token无效或无权限"
        end
        return nil, 503, "凭证服务暂不可用"
    end

    local decoded, decode_err = core.json.decode(res.body)
    if not decoded then
        core.log.error("credential ensure response decode failed: ", decode_err)
        return nil, 503, "凭证服务响应无效"
    end

    if decoded.code and decoded.code ~= 0 and decoded.code ~= 200 then
        core.log.warn("credential ensure business failed: ", decoded.code)
        return nil, 401, decoded.message or "Token无效或已过期"
    end

    return decoded.data or decoded
end

-- 判断凭证状态是否为启用。
local function credential_enabled(credential)
    if not credential then
        return false
    end

    return tonumber(credential.status) == credential_status_enable
end

-- 校验凭证状态和必要字段。
local function validate_credential(credential)
    if not credential then
        return false, 401, "Token无效或已过期"
    end

    if not credential_enabled(credential) then
        return false, 403, "凭证已禁用"
    end

    if not credential.newApiUserId or not credential.newApiUserName or not credential.apiKeyCipher then
        core.log.error("credential missing newApiUserId, newApiUserName or apiKeyCipher")
        return false, 500, "凭证配置错误"
    end

    return true
end

-- 解密new-api API Key。
local function decrypt_api_key(conf, credential, user_id)
    local cipher_version, key_id, encrypted_text = str_match(
        credential.apiKeyCipher,
        "^([^:]+):([^:]+):(.+)$"
    )
    if cipher_version ~= "v1" or not key_id or not encrypted_text then
        core.log.error("unsupported api key cipher format")
        return nil, "凭证格式错误"
    end

    local encoded_key = conf.aes_keys[key_id]
    if not encoded_key then
        core.log.error("aes key not configured for key id: ", key_id)
        return nil, "凭证密钥未配置"
    end

    local aes_key = ngx.decode_base64(encoded_key)
    local encrypted_bytes = ngx.decode_base64(encrypted_text)
    if not aes_key or #aes_key ~= 32 or not encrypted_bytes then
        core.log.error("invalid aes key or encrypted credential")
        return nil, "凭证格式错误"
    end

    if #encrypted_bytes <= aes_gcm_iv_size + aes_gcm_tag_size then
        core.log.error("encrypted credential too short")
        return nil, "凭证格式错误"
    end

    local iv = str_sub(encrypted_bytes, 1, aes_gcm_iv_size)
    local tag = str_sub(encrypted_bytes, -aes_gcm_tag_size)
    local ciphertext = str_sub(
        encrypted_bytes,
        aes_gcm_iv_size + 1,
        -aes_gcm_tag_size - 1
    )
    local aad = user_id .. ":" .. tostring(credential.newApiUserId) .. ":" .. tostring(credential.newApiUserName)

    local aes_cipher, err = cipher.new("aes-256-gcm")
    if not aes_cipher then
        core.log.error("create aes cipher failed: ", err)
        return nil, "凭证解密失败"
    end

    local api_key, decrypt_err = aes_cipher:decrypt(
        aes_key,
        iv,
        ciphertext,
        false,
        aad,
        tag
    )
    if not api_key then
        core.log.error("decrypt api key failed: ", decrypt_err)
        return nil, "凭证解密失败"
    end

    return api_key
end

-- 解码Redis URL中的转义字符。
local function decode_url_part(value)
    if not value then
        return nil
    end

    return (value:gsub("%%(%x%x)", function(hex)
        return string.char(tonumber(hex, 16))
    end))
end

-- 从Redis URL构建APISIX Redis连接配置。
build_redis_conf = function(conf)
    if not conf.redis_url then
        return conf
    end

    local scheme, address = conf.redis_url:match("^(rediss?)://(.+)$")
    if not scheme then
        return nil, "redis_url must start with redis:// or rediss://"
    end

    local authority = address
    local path = nil
    local matched_authority, matched_path = address:match("^([^/]*)(/.*)$")
    if matched_authority then
        authority = matched_authority
        path = matched_path
    end

    local userinfo, host_port = authority:match("^(.*)@(.+)$")
    if not host_port then
        host_port = authority
    end

    local username = nil
    local password = nil
    if userinfo then
        local matched_username, matched_password = userinfo:match("^([^:]*):(.*)$")
        if matched_password then
            username = decode_url_part(matched_username)
            password = decode_url_part(matched_password)
            if username == "" then
                username = nil
            end
        else
            password = decode_url_part(userinfo)
        end
    end

    local host = nil
    local port = nil
    if host_port:sub(1, 1) == "[" then
        host, port = host_port:match("^%[([^%]]+)%]:?(%d*)$")
    else
        host, port = host_port:match("^(.-):?(%d*)$")
    end
    if not host or host == "" then
        return nil, "redis_url host is required"
    end

    local database = conf.redis_database or 0
    if path and path ~= "/" then
        local matched_database = path:match("^/(%d+)$")
        if not matched_database then
            return nil, "redis_url database must be a non-negative integer"
        end
        database = tonumber(matched_database)
    end

    local redis_conf = {}
    for key, value in pairs(conf) do
        redis_conf[key] = value
    end
    redis_conf.redis_host = decode_url_part(host)
    redis_conf.redis_port = tonumber(port) or 6379
    redis_conf.redis_database = database
    redis_conf.redis_username = username or conf.redis_username
    redis_conf.redis_password = password or conf.redis_password
    redis_conf.redis_ssl = scheme == "rediss" or conf.redis_ssl

    return redis_conf
end

-- 执行请求认证与Header转换。
function _M.rewrite(conf, ctx)
    local token = read_bearer_jwt(ctx)
    if not token then
        return reject(401, "缺少Bearer Token")
    end

    local jwt_info, jwt_err = verify_gateway_jwt(conf, token)
    if not jwt_info then
        return reject(401, jwt_err)
    end

    local redis_conf, redis_conf_err = build_redis_conf(conf)
    if not redis_conf then
        core.log.error("redis config invalid: ", redis_conf_err)
        return reject(503, "凭证缓存暂不可用")
    end

    local credential, cache_state = read_redis_credential(redis_conf, jwt_info.user_id)
    if cache_state == "redis_error" then
        return reject(503, "凭证缓存暂不可用")
    end

    if not credential
            or not credential_enabled(credential) then
        local ensure_status
        local ensure_message
        credential, ensure_status, ensure_message = ensure_credential(
            conf,
            ctx,
            token,
            jwt_info.user_id
        )
        if not credential then
            return reject(ensure_status, ensure_message)
        end
    end

    local ok, status, message = validate_credential(credential)
    if not ok then
        return reject(status, message)
    end

    local api_key, decrypt_err = decrypt_api_key(
        conf,
        credential,
        jwt_info.user_id
    )
    if not api_key then
        return reject(500, decrypt_err)
    end

    core.request.set_header(ctx, "Authorization", "Bearer " .. api_key)
end

return _M
