package com.model.gateway.auth.notification.domain.service;

import com.model.gateway.auth.notification.domain.model.EmailProviderType;

public interface EmailService {
    EmailProviderType getProviderType();

    void sendVerificationCode(String toEmail, String code);
}
