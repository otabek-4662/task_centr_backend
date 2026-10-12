package com.taskcenter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility for safely normalizing Render/Heroku/PaaS postgres:// URLs into JDBC format
 * without exposing sensitive credentials in application logs.
 */
public final class DatabaseUrlNormalizer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlNormalizer.class);

    private DatabaseUrlNormalizer() {
    }

    public record NormalizedConfig(String jdbcUrl, String username, String password, String host, String dbName) {
    }

    /**
     * Normalizes a raw database connection string into JDBC format.
     * Preserves valid jdbc:... URLs without modification.
     */
    public static NormalizedConfig normalize(String rawUrl, String fallbackUser, String fallbackPass) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return new NormalizedConfig(rawUrl, fallbackUser, fallbackPass, null, null);
        }

        String trimmed = rawUrl.trim();

        // Already a standard JDBC URL
        if (trimmed.startsWith("jdbc:")) {
            return new NormalizedConfig(trimmed, fallbackUser, fallbackPass, null, null);
        }

        // Render / Heroku / PaaS style postgres:// or postgresql://
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            try {
                int schemeEnd = trimmed.indexOf("://");
                String rest = trimmed.substring(schemeEnd + 3);

                int atIndex = rest.indexOf('@');
                String userInfo = atIndex >= 0 ? rest.substring(0, atIndex) : null;
                String hostAndPath = atIndex >= 0 ? rest.substring(atIndex + 1) : rest;

                String user = fallbackUser;
                String pass = fallbackPass;

                if (userInfo != null && !userInfo.isEmpty()) {
                    int colonIndex = userInfo.indexOf(':');
                    if (colonIndex >= 0) {
                        user = decodeSafely(userInfo.substring(0, colonIndex));
                        pass = decodeSafely(userInfo.substring(colonIndex + 1));
                    } else {
                        user = decodeSafely(userInfo);
                    }
                }

                String jdbcUrl = "jdbc:postgresql://" + hostAndPath;

                int slashIndex = hostAndPath.indexOf('/');
                String host = slashIndex >= 0 ? hostAndPath.substring(0, slashIndex) : hostAndPath;
                String pathAndQuery = slashIndex >= 0 ? hostAndPath.substring(slashIndex + 1) : "";
                int qIndex = pathAndQuery.indexOf('?');
                String dbName = qIndex >= 0 ? pathAndQuery.substring(0, qIndex) : pathAndQuery;

                return new NormalizedConfig(jdbcUrl, user, pass, host, dbName);
            } catch (Exception e) {
                log.warn("Database URL normalization failed: {}. Falling back to raw URL.", e.getMessage());
                return new NormalizedConfig(trimmed, fallbackUser, fallbackPass, null, null);
            }
        }

        return new NormalizedConfig(trimmed, fallbackUser, fallbackPass, null, null);
    }

    private static String decodeSafely(String value) {
        if (value == null) return null;
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
