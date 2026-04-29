# Repository Guidelines

## Project Structure & Module Organization

This is a Spring Boot 3 / Java 17 authentication service for the model gateway.

- `src/main/java/com/model/gateway/auth`: Java source code.
- `controller`: HTTP entry points only; keep business logic in `service`.
- `service` and `service/impl`: authentication, JWT, Redis credential cache, and new-api binding logic.
- `mapper`: MyBatis mapper interfaces using annotation SQL or XML.
- `domain`, `dto`, `vo`: persistence models, request DTOs, and response objects.
- `src/main/resources/application.yml`: application configuration.
- `apisix/`: APISIX config, Dashboard config, and `model-gateway-auth.lua` plugin.
- `docs/`: deployment guide and database schema.
- `Dockerfile`, `docker-compose.yml`, `.env`: Docker deployment files.

## Build, Test, and Development Commands

- `mvn -q -DskipTests compile`: compile the project without running tests.
- `mvn test`: run Maven tests if tests are added.
- `mvn spring-boot:run`: start the service locally.
- `docker compose --env-file .env config`: validate Compose and environment interpolation.
- `docker compose --env-file .env up -d --build`: build and start Docker deployment.

## Coding Style & Naming Conventions

Use Java 17 and the existing Spring Boot conventions. Class names use `PascalCase`; methods, fields, and variables use `camelCase`. Keep controllers thin and delegate to services. Do not inject mappers directly into controllers.

New classes, methods, and fields must include concise Chinese comments. Entity classes should use Lombok `@Data`, `@Builder`, `@AllArgsConstructor`, and `@NoArgsConstructor`; do not hand-write getters and setters. Use UTF-8 for Chinese text.

For MyBatis, use `@Select("""...""")`, other annotation SQL, or XML mapper files. Do not add mapper `default` methods for database logic.

## Testing Guidelines

Unit tests are not required for current tasks, but the project should still compile before delivery. Run:

```bash
mvn -q -DskipTests compile
```

When Docker or APISIX config changes, also run:

```bash
docker compose --env-file .env config
```

## Commit & Pull Request Guidelines

Follow the existing commit style:

```text
feat(config): 添加环境配置文件支持数据库和网关设置
docs(deploy): 更新部署文档中APISIX路由配置和密钥处理说明
refactor(auth): 将管理员用户管理功能合并到用户资料服务中
```

Use a short type and scope, then a clear Chinese description. PRs should include the purpose, affected endpoints/config files, verification commands, and any deployment notes.

## Security & Configuration Tips

Do not commit real JWT private keys, AES keys, APISIX secrets, or new-api API keys. `.env` is for local deployment values and must be reviewed before publishing. PEM keys in `.env` must be stored as one-line `\n` escaped values. User and new-api bindings are manually maintained in MySQL; avoid inventing fallback credentials or hard-coded magic values.

## 要求
apisix 后续是单独部署，所以docker脚本配置都是单独的， 不要放在最外层