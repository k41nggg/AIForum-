package com.zhidao.demo.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单帖、单用户的最近三轮问答记忆：只保留最近 N 轮 Q/A（默认 3），并设置 TTL 防止内存无限增长。
 */
@Service
public class PostQaMemoryService {

    public record QaPair(String question, String answer) {
    }

    public record Memory(Deque<QaPair> history, long updateAtMillis) {
    }

    private final Map<String, Memory> store = new ConcurrentHashMap<>();

    /**
     * 记忆有效期（超时自动失效）
     */
    private static final Duration TTL = Duration.ofMinutes(30);

    /**
     * 每个帖子/用户最多保留的问答轮数
     */
    private static final int MAX_TURNS = 3;

    public Memory get(Long postId, Long userId) {
        if (postId == null || userId == null) return null;
        String key = key(postId, userId);
        Memory m = store.get(key);
        if (m == null) return null;

        long now = System.currentTimeMillis();
        if (now - m.updateAtMillis() > TTL.toMillis()) {
            store.remove(key);
            return null;
        }
        return m;
    }

    public void put(Long postId, Long userId, String question, String answer) {
        if (postId == null || userId == null) return;
        String q = safe(question);
        String a = safe(answer);
        if (q.isBlank() || a.isBlank()) return;

        String key = key(postId, userId);
        store.compute(key, (k, old) -> {
            Deque<QaPair> deque;
            if (old == null || old.history() == null) {
                deque = new ArrayDeque<>();
            } else {
                // 拷贝一份，避免直接复用旧引用导致潜在并发问题
                deque = new ArrayDeque<>(old.history());
            }

            deque.addLast(new QaPair(q, a));
            while (deque.size() > MAX_TURNS) {
                deque.removeFirst();
            }

            return new Memory(deque, System.currentTimeMillis());
        });
    }

    public void clear(Long postId, Long userId) {
        if (postId == null || userId == null) return;
        store.remove(key(postId, userId));
    }

    private static String key(Long postId, Long userId) {
        return postId + ":" + userId;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
