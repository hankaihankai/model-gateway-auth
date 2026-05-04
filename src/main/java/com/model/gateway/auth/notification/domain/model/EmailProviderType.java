package com.model.gateway.auth.notification.domain.model;

import java.util.Set;

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
