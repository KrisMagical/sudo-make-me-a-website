package com.magiccode.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Long> ipLastCommentTime = new ConcurrentHashMap<>();
    private final Map<String, Long> ipBlockUntil = new ConcurrentHashMap<>();
    private static final long INTERVAL_MS = 1000;          // 1 秒内最多一次
    private static final long BLOCK_DURATION_MS = 60000;   // 惩罚时长：1 分钟

    public boolean tryAcquire(String ip) {
        long now = System.currentTimeMillis();

        Long blockUntil = ipBlockUntil.get(ip);
        if (blockUntil != null && blockUntil > now) {
            return false;
        }

        Long last = ipLastCommentTime.get(ip);
        if (last != null && (now - last) < INTERVAL_MS) {
            ipBlockUntil.put(ip, now + BLOCK_DURATION_MS);
            ipLastCommentTime.remove(ip);
            return false;
        }

        ipLastCommentTime.put(ip, now);
        return true;
    }

    @Scheduled(fixedDelay = 3600000)
    public void cleanUp() {
        long now = System.currentTimeMillis();
        ipLastCommentTime.entrySet().removeIf(entry -> (now - entry.getValue()) > BLOCK_DURATION_MS);
        ipBlockUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}