package com.model.gateway.auth.email.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

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
        if (config == null) {
            return new JavaMailSenderImpl();
        }
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
