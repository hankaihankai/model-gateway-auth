# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`model-gateway-auth` is a Spring Boot 3.3.7 / Java 17 authentication service for the model gateway. It provides user registration, login (Sa-Token JWT), admin user management, gateway credential issuance, and new-api user binding. The codebase follows a lightweight DDD directory structure with 3 bounded contexts.

## Build & Run

```bash
# Compile (tests are not required to pass, but compilation must succeed)
mvn -q -DskipTests compile

# Run locally (requires MySQL + Redis; see .env)
mvn spring-boot:run

# Validate Docker Compose config
docker compose --env-file .env config

# Build and deploy with Docker
docker compose --env-file .env up -d --build
```

## Architecture

The project uses a lightweight DDD four-layer structure inside each bounded context. Dependencies flow top-down: `interfaces` → `application` → `domain` → `infrastructure`. Cross-BC calls go through the `application` layer only; never call another BC's mapper or domain classes directly.

```
com.model.gateway.auth
├── config              # App-level cross-cutting config: Sa-Token, JWT, Password
├── identity            # Core BC: users, login sessions, gateway credentials
│   ├── interfaces/{http,dto,vo}
│   ├── application
│   ├── domain/{model,service}
│   └── infrastructure/{persistence/mapper,cache,config}
├── newapi              # ACL BC: new-api external system integration
│   ├── application
│   ├── domain/model
│   └── infrastructure/{persistence/mapper,external,config}
├── notification        # Generic BC: email verification codes
│   ├── domain/{model,service,exception}
│   ├── application
│   └── infrastructure/{sender,config}
└── shared              # Shared kernel: ApiResponse, exceptions, enums, utils
    ├── api, constant, enums, exception, util
```

Top-level `config/` holds application-wide beans (`SaTokenConfig`, `PasswordConfig`, `RsaSaJwtTemplate`) that don't belong to any single BC. MyBatis mappers are scanned via `@MapperScan` on `ModelGatewayAuthApplication`.

## Coding Style

- Java 17, Spring Boot conventions. PascalCase classes, camelCase methods/fields.
- New classes, methods, and fields must include concise **Chinese comments**.
- Entity classes use Lombok `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`. Do not hand-write getters/setters.
- MyBatis: use `@Select("""...""")`, other annotation SQL, or XML mappers. Do not add mapper `default` methods for database logic.
- Keep controllers thin; do not inject mappers directly into controllers.

## Testing

Unit tests are not required for current tasks, but the project must compile before delivery.

```bash
mvn -q -DskipTests compile
```

## Commit Style

Use Chinese conventional commits:

```text
feat(config): 添加环境配置文件支持数据库和网关设置
docs(deploy): 更新部署文档中APISIX路由配置和密钥处理说明
refactor(auth): 将管理员用户管理功能合并到用户资料服务中
```

## Security & Configuration

- Do not commit real JWT private keys, AES keys, APISIX secrets, or new-api API keys.
- `.env` is for local deployment values. PEM keys in `.env` must be stored as one-line `\n` escaped values.
- APISIX is deployed separately; its Docker configs live in `apisix/` and should not be moved to the repository root.