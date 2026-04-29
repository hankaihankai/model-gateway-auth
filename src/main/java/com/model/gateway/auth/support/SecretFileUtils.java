package com.model.gateway.auth.support;

import com.model.gateway.auth.exception.AuthException;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 密钥文件读取工具。
 */
public final class SecretFileUtils {

    /**
     * 私有构造方法。
     */
    private SecretFileUtils() {
    }

    /**
     * 读取必填密钥文件原文。
     *
     * @param filePath 密钥文件路径
     * @param configName 配置名称
     * @return 密钥文件内容
     */
    public static String readRequiredText(String filePath, String configName) {
        if (!StringUtils.hasText(filePath)) {
            throw new AuthException(configName + "文件路径未配置");
        }
        try {
            String text = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            if (!StringUtils.hasText(text)) {
                throw new AuthException(configName + "文件内容为空");
            }
            return text;
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException(configName + "文件读取失败");
        }
    }

    /**
     * 读取必填密钥文件并去除首尾空白。
     *
     * @param filePath 密钥文件路径
     * @param configName 配置名称
     * @return 去除首尾空白后的密钥内容
     */
    public static String readRequiredTrimmed(String filePath, String configName) {
        return readRequiredText(filePath, configName).trim();
    }
}
