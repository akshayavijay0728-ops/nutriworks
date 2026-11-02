package com.nutriworks.server;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe manager for ephemeral phone connection tokens.
 * Enforces 15-minute token expiry, token regeneration, and device session tracking.
 */
public class BridgeTokenManager {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long DEFAULT_EXPIRY_MILLIS = TimeUnit.MINUTES.toMillis(15);

    public static class TokenInfo {
        private final String token;
        private final long createdAt;
        private final long expiresAt;
        private volatile String lastConnectedDevice = "None";
        private volatile long lastSeen = 0;

        public TokenInfo(String token, long expiryMillis) {
            this.token = token;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = this.createdAt + expiryMillis;
        }

        public String getToken() {
            return token;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        public long getRemainingSeconds() {
            long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        }

        public String getLastConnectedDevice() {
            return lastConnectedDevice;
        }

        public void updateDevice(String userAgent) {
            this.lastConnectedDevice = userAgent != null && !userAgent.isEmpty() ? userAgent : "Mobile Browser";
            this.lastSeen = System.currentTimeMillis();
        }

        public long getLastSeen() {
            return lastSeen;
        }
    }

    private volatile TokenInfo currentToken;
    private final ConcurrentHashMap<String, TokenInfo> activeTokens = new ConcurrentHashMap<>();

    public BridgeTokenManager() {
        regenerateToken(15);
    }

    public synchronized TokenInfo regenerateToken(int timeoutMinutes) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        long expiry = timeoutMinutes > 0 ? TimeUnit.MINUTES.toMillis(timeoutMinutes) : DEFAULT_EXPIRY_MILLIS;
        TokenInfo info = new TokenInfo(token, expiry);

        currentToken = info;
        activeTokens.put(token, info);
        return info;
    }

    public TokenInfo getCurrentToken() {
        return currentToken;
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        TokenInfo info = activeTokens.get(token.trim());
        return info != null && !info.isExpired();
    }

    public TokenInfo getTokenInfo(String token) {
        return token != null ? activeTokens.get(token.trim()) : null;
    }
}
