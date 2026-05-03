package com.model.gateway.auth.email;

import com.model.gateway.auth.email.exception.EmailSendException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (service == null) {
            throw new EmailSendException("没有可用的邮件服务商: " + toEmail, null);
        }
        service.sendVerificationCode(toEmail, code);
    }

    private String extractDomain(String email) {
        int at = email.lastIndexOf('@');
        return at < 0 ? "" : email.substring(at + 1).toLowerCase();
    }
}
