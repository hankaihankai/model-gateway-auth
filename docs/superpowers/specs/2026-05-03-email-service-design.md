# 邮件发送服务设计文档

## 背景与目标

项目需要新增用户注册/登录的验证码邮件发送功能。要求：
- 支持多个邮件服务商（QQ、163、通用 SMTP 等），按用户邮箱后缀自动路由到对应服务商。
- 采用"接口 + 多实现类"的设计，实现类对应不同邮箱服务商。
- 纯文本邮件，仅用于发送验证码。
- 配置沿用项目现有的 `@ConfigurationProperties` + 环境变量风格。

## 方案对比

| 方案 | 核心思路 | 优点 | 缺点 |
|------|----------|------|------|
| A：策略模式 + 自定义注解 | 用 `@EmailProvider` 注解标识实现类，扫描构建 Map | 扩展性最好 | 需要反射扫描，稍复杂 |
| **B：枚举映射 + List注入（推荐）** | `EmailService` 接口声明类型，工厂注入 `List<EmailService>` 后按枚举建 Map | 简单直观，符合 Spring 风格，完全满足需求 | 新增服务商需要新增枚举值和实现类 |
| C：单一实现类 | 一个类内部维护多个 `JavaMailSender` | 代码集中 | 违背"不同实现类"的设计意图 |

最终采用 **方案 B**：接口定义 `getProviderType()` 方法，工厂注入所有实现后自动构建路由 Map。

## 详细设计

### 文件结构

```
src/main/java/com/model/gateway/auth/email/
├── EmailProviderType.java         # 枚举：标识邮件服务商及其域名后缀
├── EmailService.java              # 接口：定义发送能力和类型声明
├── EmailFactory.java              # 工厂：注入所有实现，构造方法建 Map，自动后缀路由
├── QqEmailService.java            # QQ 邮箱实现
├── NeteaseEmailService.java       # 163 邮箱实现
├── GenericEmailService.java       # 通用 SMTP 兜底实现
├── config/
│   └── EmailProperties.java       # 多服务商配置绑定
│   └── EmailSenderConfig.java     # 多个 JavaMailSender Bean 注册
└── exception/
    └── EmailSendException.java    # 邮件相关异常
```

### 1. EmailProviderType（枚举）

```java
public enum EmailProviderType {
    QQ("qq.com", "foxmail.com"),
    NETEASE("163.com", "126.com", "yeah.net"),
    GENERIC();

    private final Set<String> domains;

    EmailProviderType(String... domains) {
        this.domains = Set.of(domains);
    }

    public Set<String> getDomains() {
        return domains;
    }
}
```

### 2. EmailService（接口）

```java
public interface EmailService {
    EmailProviderType getProviderType();
    void sendVerificationCode(String toEmail, String code);
}
```

### 3. EmailFactory（工厂）

```java
@Service
public class EmailFactory {
    private final Map<EmailProviderType, EmailService> serviceMap;
    private final Map<String, EmailProviderType> domainMap;
    private final EmailService genericService;

    public EmailFactory(List<EmailService> services) {
        this.serviceMap = new EnumMap<>(EmailProviderType.class);
        this.domainMap = new HashMap<>();
        for (EmailService s : services) {
            EmailProviderType type = s.getProviderType();
            serviceMap.put(type, s);
            for (String domain : type.getDomains()) {
                domainMap.put(domain.toLowerCase(), type);
            }
        }
        this.genericService = serviceMap.get(EmailProviderType.GENERIC);
    }

    public void sendVerificationCode(String toEmail, String code) {
        String domain = extractDomain(toEmail);
        EmailProviderType type = domainMap.getOrDefault(domain, EmailProviderType.GENERIC);
        EmailService service = serviceMap.get(type);
        if (service == null) {
            service = genericService;
        }
        service.sendVerificationCode(toEmail, code);
    }

    private String extractDomain(String email) {
        int at = email.lastIndexOf('@');
        return at < 0 ? "" : email.substring(at + 1).toLowerCase();
    }
}
```

### 4. 实现类（以 QQ 为例）

```java
@Service
public class QqEmailService implements EmailService {
    private final JavaMailSender mailSender;
    private final EmailProperties props;

    public QqEmailService(
            @Qualifier("qqMailSender") JavaMailSender mailSender,
            EmailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public EmailProviderType getProviderType() {
        return EmailProviderType.QQ;
    }

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.getQq().getFrom());
            message.setTo(toEmail);
            message.setSubject("验证码");
            message.setText("您的验证码是：" + code);
            mailSender.send(message);
        } catch (MailException e) {
            throw new EmailSendException("邮件发送失败: " + toEmail, e);
        }
    }
}
```

其他实现类（`NeteaseEmailService`、`GenericEmailService`）结构完全一致，只是注入的 `JavaMailSender` Bean 名称和 `getProviderType()` 返回值不同。

### 5. 配置类

**EmailProperties.java**：

```java
@ConfigurationProperties(prefix = "email")
@Data
public class EmailProperties {
    private SmtpConfig qq;
    private SmtpConfig netease;
    private SmtpConfig generic;

    @Data
    public static class SmtpConfig {
        private String host;
        private int port;
        private String username;
        private String password;
        private String from;
    }
}
```

**EmailSenderConfig.java**：

```java
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailSenderConfig {

    @Bean
    public JavaMailSender qqMailSender(EmailProperties props) {
        return createSender(props.getQq());
    }

    @Bean
    public JavaMailSender neteaseMailSender(EmailProperties props) {
        return createSender(props.getNetease());
    }

    @Bean
    public JavaMailSender genericMailSender(EmailProperties props) {
        return createSender(props.getGeneric());
    }

    private JavaMailSender createSender(EmailProperties.SmtpConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        Properties p = new Properties();
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.starttls.enable", "true");
        sender.setJavaMailProperties(p);
        return sender;
    }
}
```

### 6. 异常

```java
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 7. application.yml 配置示例

```yaml
email:
  qq:
    host: smtp.qq.com
    port: 587
    username: ${EMAIL_QQ_USERNAME:}
    password: ${EMAIL_QQ_PASSWORD:}
    from: ${EMAIL_QQ_FROM:}
  netease:
    host: smtp.163.com
    port: 25
    username: ${EMAIL_163_USERNAME:}
    password: ${EMAIL_163_PASSWORD:}
    from: ${EMAIL_163_FROM:}
  generic:
    host: ${EMAIL_GENERIC_HOST:}
    port: ${EMAIL_GENERIC_PORT:587}
    username: ${EMAIL_GENERIC_USERNAME:}
    password: ${EMAIL_GENERIC_PASSWORD:}
    from: ${EMAIL_GENERIC_FROM:}
```

## 依赖

在 `pom.xml` 中新增：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## 调用方式

```java
@Service
public class AuthServiceImpl {
    private final EmailFactory emailFactory;

    public AuthServiceImpl(EmailFactory emailFactory) {
        this.emailFactory = emailFactory;
    }

    public void sendRegisterCode(String email) {
        String code = generateCode();
        emailFactory.sendVerificationCode(email, code);
    }
}
```

## 可选扩展（预留，不实现）

- **@Async 异步发送**：给 `EmailFactory.sendVerificationCode()` 或各实现类方法加 `@Async`，避免 SMTP 连接阻塞用户请求。
- **验证码缓存**：发送后将 `code` 存入 Redis，有效期 5 分钟，注册时取出校验。
