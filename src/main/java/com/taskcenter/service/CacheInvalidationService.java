package com.taskcenter.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class CacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);

    private final CacheManager cacheManager;

    public CacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Workspace o'chirilganda yoki katta o'zgarishlarda ushbu workspacening barcha keshlarini ommaviy tozalash (Bulk evict).
     */
    public void evictWorkspace(String workspaceId) {
        if (workspaceId == null || cacheManager == null) return;
        log.info("Workspace uchun keshlar ommaviy tozalanmoqda (bulk evict): {}", workspaceId);

        // 1. Workspace asosiy keshidan o'chirish
        evictKey("workspaces", workspaceId);

        // 2. Workspace a'zolari va rollarini tozalash
        evictPrefix("workspaceRoles", workspaceId + ":");
        evictKey("workspaceMembers", workspaceId);

        // 3. Columns, Labels, Sprints, Stats keshlarini tozalash
        evictKey("columns", workspaceId);
        evictKey("labels", workspaceId);
        evictPrefix("sprints", workspaceId);
        evictPrefix("sprintStats", workspaceId);
        evictPrefix("taskStats", workspaceId);
    }

    /**
     * Sprint yopilganda (complete) yoki o'chirilganda sprint va unga tegishli vazifa/statistika keshlarini tozalash.
     */
    public void evictSprint(String workspaceId, String sprintId) {
        if (cacheManager == null) return;
        log.info("Sprint uchun keshlar tozalanmoqda: workspace={}, sprint={}", workspaceId, sprintId);

        if (workspaceId != null) {
            evictKey("sprints", workspaceId);
            evictPrefix("sprintStats", workspaceId);
            evictPrefix("taskStats", workspaceId);
        }
        if (sprintId != null) {
            evictKey("sprints", sprintId);
            evictKey("sprintStats", sprintId);
        }
    }

    /**
     * Bitta foydalanuvchi ma'lumotlari keshdan tozalash.
     */
    public void evictUser(String username, String userId) {
        if (cacheManager == null) return;
        if (username != null) evictKey("users", username);
        if (userId != null) evictKey("users", userId);
    }

    /**
     * Tizimdagi barcha keshni to'liq tozalash.
     */
    public void evictAll() {
        if (cacheManager == null) return;
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("Barcha tizim keshlari to'liq tozalandi.");
    }

    public void evictKey(String cacheName, Object key) {
        if (cacheManager == null || cacheName == null || key == null) return;
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    @SuppressWarnings("unchecked")
    private void evictPrefix(String cacheName, String prefix) {
        if (cacheManager == null || cacheName == null || prefix == null) return;
        org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
        if (springCache == null) return;

        Object nativeCache = springCache.getNativeCache();
        if (nativeCache instanceof Cache<?, ?> caffeineCache) {
            caffeineCache.asMap().keySet().removeIf(key -> key != null && key.toString().startsWith(prefix));
        } else {
            springCache.clear();
        }
    }
}
