package com.model.gateway.auth.notification.infrastructure.sender;

import com.model.gateway.auth.notification.infrastructure.config.EmailProperties;
import com.model.gateway.auth.notification.domain.exception.EmailSendException;
import com.model.gateway.auth.notification.domain.model.EmailProviderType;
import com.model.gateway.auth.notification.domain.service.EmailService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class GenericEmailService implements EmailService {
    private final JavaMailSender mailSender;
    private final EmailProperties props;

    public GenericEmailService(
            @Qualifier("genericMailSender") JavaMailSender mailSender,
            EmailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public EmailProviderType getProviderType() {
        return EmailProviderType.GENERIC;
    }

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.getGeneric().getFrom());
            message.setTo(toEmail);
            message.setSubject("验证码");
            message.setText("您的验证码是：" + code);
            mailSender.send(message);
        } catch (MailException e) {
            throw new EmailSendException("邮件发送失败: " + toEmail, e);
        }
    }
}
