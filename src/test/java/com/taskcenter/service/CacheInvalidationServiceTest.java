package com.taskcenter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.*;

class CacheInvalidationServiceTest {

    private CacheManager cacheManager;
    private CacheInvalidationService cacheInvalidationService;

    @BeforeEach
    void setUp() {
        cacheManager = new CaffeineCacheManager(
                "workspaces", "workspaceRoles", "workspaceMembers", "columns", "labels", "sprints", "users", "sprintStats", "taskStats"
        );
        cacheInvalidationService = new CacheInvalidationService(cacheManager);
    }

    @Test
    void testEvictWorkspace_BulkEvictsAllRelatedCaches() {
        Cache wsCache = cacheManager.getCache("workspaces");
        Cache roleCache = cacheManager.getCache("workspaceRoles");
        Cache colCache = cacheManager.getCache("columns");
        Cache labelCache = cacheManager.getCache("labels");

        wsCache.put("ws-100", "Workspace Data");
        roleCache.put("ws-100:user-1", "OWNER");
        roleCache.put("ws-100:user-2", "MEMBER");
        roleCache.put("ws-200:user-1", "VIEWER"); // Other workspace
        colCache.put("ws-100", "Columns List");
        labelCache.put("ws-100", "Labels List");

        cacheInvalidationService.evictWorkspace("ws-100");

        assertNull(wsCache.get("ws-100"));
        assertNull(roleCache.get("ws-100:user-1"));
        assertNull(roleCache.get("ws-100:user-2"));
        assertNotNull(roleCache.get("ws-200:user-1")); // Other workspace remains untouched
        assertNull(colCache.get("ws-100"));
        assertNull(labelCache.get("ws-100"));
    }

    @Test
    void testEvictSprint_EvictsSprintAndStats() {
        Cache sprintCache = cacheManager.getCache("sprints");
        Cache statsCache = cacheManager.getCache("sprintStats");

        sprintCache.put("sprint-1", "Sprint Data");
        statsCache.put("sprint-1", "Stats Data");

        cacheInvalidationService.evictSprint("ws-1", "sprint-1");

        assertNull(sprintCache.get("sprint-1"));
        assertNull(statsCache.get("sprint-1"));
    }

    @Test
    void testEvictAll_ClearsAllCaches() {
        Cache wsCache = cacheManager.getCache("workspaces");
        Cache userCache = cacheManager.getCache("users");

        wsCache.put("ws-1", "Data 1");
        userCache.put("user-1", "Data 2");

        cacheInvalidationService.evictAll();

        assertNull(wsCache.get("ws-1"));
        assertNull(userCache.get("user-1"));
    }
}
