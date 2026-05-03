package com.model.gateway.auth.email;

public interface EmailService {
    EmailProviderType getProviderType();

    void sendVerificationCode(String toEmail, String code);
}
