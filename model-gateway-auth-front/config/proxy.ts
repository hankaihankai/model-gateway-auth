/**
 * 开发环境代理：把所有 /model-gateway-auth 转发到本地 9080 后端，
 * /v1/chat/completions 直接转发到 APISIX 网关。
 */
export default {
  dev: {
    '/model-gateway-auth': {
      target: 'http://localhost:9080/model-gateway-auth',
      changeOrigin: true,
      pathRewrite: { '^/model-gateway-auth': '' },
    },
    '/v1/chat/completions': {
      target: 'http://localhost:9080',
      changeOrigin: true,
    },
  },
};
