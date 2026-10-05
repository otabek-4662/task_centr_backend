package com.taskcenter.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

    private static class LimitInfo {
        int count;
        long windowResetTimeMillis;

        LimitInfo(long windowResetTimeMillis) {
            this.count = 1;
            this.windowResetTimeMillis = windowResetTimeMillis;
        }
    }

    private final Map<String, LimitInfo> ipLimits = new ConcurrentHashMap<>();
    private final Map<String, LimitInfo> userLimits = new ConcurrentHashMap<>();
    private final Map<String, LimitInfo> usernameFailedLimits = new ConcurrentHashMap<>();

    @Value("${ratelimit.login.username.max-failed:5}")
    private int loginUsernameMaxFailed;
    @Value("${ratelimit.login.username.window-minutes:15}")
    private int loginUsernameWindow;

    // Checks and increments if allowed
    // Returns 0 if allowed, or time-to-wait in seconds if blocked
    public long tryConsumeIpLimit(String endpoint, String ip, int maxRequests, int windowMinutes) {
        String key = endpoint + ":" + ip;
        return tryConsume(ipLimits, key, maxRequests, windowMinutes * 60000L);
    }

    public long tryConsumeUserLimit(String endpoint, String userId, int maxRequests, int windowMinutes) {
        String key = endpoint + ":" + userId;
        return tryConsume(userLimits, key, maxRequests, windowMinutes * 60000L);
    }

    public long checkUsernameFailedLimit(String username) {
        if (username == null) return 0;
        String key = username.toLowerCase();
        LimitInfo info = usernameFailedLimits.get(key);
        long now = System.currentTimeMillis();
        if (info != null && now < info.windowResetTimeMillis) {
            if (info.count >= loginUsernameMaxFailed) {
                return Math.max(1, (info.windowResetTimeMillis - now) / 1000);
            }
        }
        return 0;
    }

    public void recordFailedLogin(String username) {
        if (username == null) return;
        String key = username.toLowerCase();
        long now = System.currentTimeMillis();
        long windowMillis = loginUsernameWindow * 60000L;
        
        LimitInfo finalInfo = usernameFailedLimits.compute(key, (k, info) -> {
            if (info == null || now >= info.windowResetTimeMillis) {
                return new LimitInfo(now + windowMillis);
            }
            if (info.count < loginUsernameMaxFailed + 100000) {
                info.count++;
            }
            return info;
        });
        
        if (finalInfo != null && finalInfo.count == loginUsernameMaxFailed) {
            log.warn("Rate limit breached: user {} exceeded failed login attempts", username);
        }
    }

    public void resetFailedLogin(String username) {
        if (username == null) return;
        usernameFailedLimits.remove(username.toLowerCase());
    }

    private long tryConsume(Map<String, LimitInfo> map, String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        long[] waitTime = new long[1];
        
        map.compute(key, (k, info) -> {
            if (info == null || now >= info.windowResetTimeMillis) {
                return new LimitInfo(now + windowMillis);
            }
            if (info.count >= maxRequests) {
                if (info.count < maxRequests + 100000) {
                    info.count++;
                }
                waitTime[0] = Math.max(1, (info.windowResetTimeMillis - now) / 1000);
                if (info.count == maxRequests + 1 || (info.count - maxRequests) % 50 == 0) {
                    log.warn("Rate limit breached for key: {}", key);
                }
                return info;
            }
            info.count++;
            return info;
        });

        return waitTime[0];
    }

    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        ipLimits.entrySet().removeIf(e -> now >= e.getValue().windowResetTimeMillis);
        userLimits.entrySet().removeIf(e -> now >= e.getValue().windowResetTimeMillis);
        usernameFailedLimits.entrySet().removeIf(e -> now >= e.getValue().windowResetTimeMillis);
    }
}
