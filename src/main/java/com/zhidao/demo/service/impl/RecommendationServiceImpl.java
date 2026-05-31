package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.ChatResponse;
import com.zhidao.demo.dto.Message;
import com.zhidao.demo.dto.RecommendationResponse;
import com.zhidao.demo.entity.*;
import com.zhidao.demo.mapper.PostMapper;
import com.zhidao.demo.mapper.UserActionMapper;
import com.zhidao.demo.mapper.UserFollowMapper;
import com.zhidao.demo.mapper.UserMapper;
import com.zhidao.demo.mapper.UserRecommendationMapper;
import com.zhidao.demo.service.AIService;
import com.zhidao.demo.service.CategoryService;
import com.zhidao.demo.service.RecommendationService;
import com.zhidao.demo.service.SubscriptionService;
import com.zhidao.demo.util.AiTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private static final int ACTION_LIMIT = 50;
    private static final int CANDIDATE_LIMIT = 40;
    private static final int MIN_RECOMMEND = 5;
    private static final int MAX_RECOMMEND = 10;
    private static final int EXCERPT_CHARS = 200;

    @Autowired
    private UserRecommendationMapper recommendationMapper;

    @Autowired
    private UserActionMapper userActionMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AIService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RecommendationResponse getRecommendations(User user) {
        RecommendationResponse resp = new RecommendationResponse();
        resp.setCached(false);
        resp.setPosts(List.of());

        UserRecommendation cache = getCacheRow(user.getId());
        if (cache == null) {
            resp.setHint("点击「AI 刷新推荐」生成个性化列表");
            return resp;
        }

        fillFromCache(resp, cache);
        return resp;
    }

    @Override
    @Transactional
    public RecommendationResponse refreshRecommendations(User user) {
        BehaviorSnapshot snapshot = buildBehaviorSnapshot(user.getId());
        List<Post> candidates = buildCandidatePosts(user.getId(), snapshot.interactedPostIds());
        List<Long> recommendedIds;
        String summary;

        if (candidates.isEmpty()) {
            recommendedIds = List.of();
            summary = "暂无合适候选帖子，请稍后再试";
        } else {
            summary = resolvePreferenceSummary(snapshot);
            AiRecommend ai = callAiRecommend(snapshot, candidates);
            recommendedIds = validateAndOrderIds(ai.postIds, candidates);
            if (recommendedIds.size() < MIN_RECOMMEND) {
                recommendedIds = padFromCandidates(recommendedIds, candidates, MIN_RECOMMEND);
            }
        }

        saveCache(user.getId(), recommendedIds, summary, snapshot.actionCount());

        UserRecommendation saved = getCacheRow(user.getId());
        RecommendationResponse resp = new RecommendationResponse();
        fillFromCache(resp, saved);
        return resp;
    }

    private void fillFromCache(RecommendationResponse resp, UserRecommendation cache) {
        List<Long> ids = parsePostIds(cache.getPostIds());
        resp.setPosts(loadPostsByIds(ids));
        resp.setCached(!ids.isEmpty());
        resp.setUpdatedAt(cache.getUpdateTime());
        resp.setSummary(cache.getSummary());
        resp.setActionCount(cache.getActionCount());
        if (ids.isEmpty()) {
            resp.setHint("点击「AI 刷新推荐」生成个性化列表");
        }
    }

    private UserRecommendation getCacheRow(Long userId) {
        return recommendationMapper.selectOne(new LambdaQueryWrapper<UserRecommendation>()
                .eq(UserRecommendation::getUserId, userId));
    }

    private void saveCache(Long userId, List<Long> postIds, String summary, int actionCount) {
        String json;
        try {
            json = objectMapper.writeValueAsString(postIds);
        } catch (Exception e) {
            json = "[]";
        }

        UserRecommendation row = getCacheRow(userId);
        if (row == null) {
            row = new UserRecommendation();
            row.setUserId(userId);
            row.setPostIds(json);
            row.setSummary(summary);
            row.setActionCount(actionCount);
            recommendationMapper.insert(row);
        } else {
            row.setPostIds(json);
            row.setSummary(summary);
            row.setActionCount(actionCount);
            recommendationMapper.updateById(row);
        }
    }

    private List<Long> parsePostIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            return ids == null ? List.of() : ids;
        } catch (Exception e) {
            log.warn("解析推荐 post_ids 失败: {}", json, e);
            return List.of();
        }
    }

    private BehaviorSnapshot buildBehaviorSnapshot(Long userId) {
        List<UserAction> actions = userActionMapper.selectList(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, userId)
                .in(UserAction::getType, UserAction.TYPE_LIKE_POST, UserAction.TYPE_COLLECT_POST)
                .orderByDesc(UserAction::getCreateTime)
                .last("LIMIT " + ACTION_LIMIT));

        Set<Long> interacted = new LinkedHashSet<>();
        List<String> likedLines = new ArrayList<>();
        List<String> collectedLines = new ArrayList<>();

        Map<Long, Category> categoryMap = categoryService.list().stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        for (UserAction action : actions) {
            interacted.add(action.getTargetId());
            Post post = postMapper.selectById(action.getTargetId());
            if (post == null) continue;
            String cat = categoryName(categoryMap, post.getCategoryId());
            String line = formatPostLine(post, cat);
            if (action.getType() == UserAction.TYPE_LIKE_POST) {
                likedLines.add(line);
            } else if (action.getType() == UserAction.TYPE_COLLECT_POST) {
                collectedLines.add(line);
            }
        }

        List<Subscription> subs = subscriptionService.list(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getUserId, userId));
        List<String> subCats = subs.stream()
                .map(s -> categoryName(categoryMap, s.getCategoryId()))
                .filter(n -> !n.isBlank())
                .distinct()
                .collect(Collectors.toList());

        List<UserFollow> follows = userFollowMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .last("LIMIT 20"));
        List<String> followNames = new ArrayList<>();
        for (UserFollow f : follows) {
            User u = userMapper.selectById(f.getFolloweeId());
            if (u != null) {
                followNames.add(u.getNickname() != null && !u.getNickname().isBlank()
                        ? u.getNickname() : u.getUsername());
            }
        }

        Map<String, Integer> categoryHits = new LinkedHashMap<>();
        for (UserAction action : actions) {
            Post post = postMapper.selectById(action.getTargetId());
            if (post == null) continue;
            String cat = categoryName(categoryMap, post.getCategoryId());
            if (!cat.isBlank()) {
                categoryHits.merge(cat, 1, Integer::sum);
            }
        }
        List<String> topCategories = categoryHits.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(e -> e.getKey() + "（" + e.getValue() + "次）")
                .collect(Collectors.toList());

        return new BehaviorSnapshot(actions.size(), interacted, likedLines, collectedLines, subCats, followNames, topCategories);
    }

    /** 优先使用独立 AI 兴趣总结；失败时用规则句，避免空泛默认文案 */
    private String resolvePreferenceSummary(BehaviorSnapshot snapshot) {
        String aiSummary = callAiPreferenceSummary(snapshot);
        if (aiSummary != null && !aiSummary.isBlank()) {
            return aiSummary.trim();
        }
        return buildRulePreferenceSummary(snapshot);
    }

    private String callAiPreferenceSummary(BehaviorSnapshot snapshot) {
        ChatRequest req = new ChatRequest();
        req.setModel(null);
        req.setMessages(Arrays.asList(
                new Message("system",
                        "你是论坛用户兴趣分析助手。根据用户的点赞、收藏、订阅与关注记录，用中文写恰好一句话概括其内容偏好。"
                                + "必须采用类似「您喜欢的类型是……」「您常关注……类话题」的句式，点出具体分类或主题关键词。"
                                + "禁止空泛表述（如「根据你的兴趣推荐」「为你精心挑选」）。只输出这一句话，不要 JSON、不要列表、不要引号包裹。"),
                new Message("user", buildProfilePrompt(snapshot))
        ));
        try {
            String raw = aiService.getCompletion(req)
                    .map(r -> r.getChoices().get(0).getMessage().getContent())
                    .block();
            if (raw == null) return "";
            String text = raw.trim();
            if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
                text = text.substring(1, text.length() - 1).trim();
            }
            return text;
        } catch (Exception e) {
            log.warn("AI 兴趣总结失败", e);
            return "";
        }
    }

    private String buildProfilePrompt(BehaviorSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("【互动分类统计】\n");
        if (snapshot.topCategories().isEmpty()) {
            sb.append("  （点赞/收藏中暂无分类数据）\n");
        } else {
            sb.append("  ").append(String.join("、", snapshot.topCategories())).append("\n");
        }
        sb.append("【订阅分类】").append(snapshot.subCategories().isEmpty() ? "无" : String.join("、", snapshot.subCategories())).append("\n");
        sb.append("【关注用户】").append(snapshot.followNames().isEmpty() ? "无" : String.join("、", snapshot.followNames())).append("\n");
        sb.append("【点赞帖子样本】\n");
        if (snapshot.likedLines().isEmpty()) sb.append("  （无）\n");
        else snapshot.likedLines().stream().limit(8).forEach(l -> sb.append("  · ").append(l).append("\n"));
        sb.append("【收藏帖子样本】\n");
        if (snapshot.collectedLines().isEmpty()) sb.append("  （无）\n");
        else snapshot.collectedLines().stream().limit(8).forEach(l -> sb.append("  · ").append(l).append("\n"));
        sb.append("\n请用一句话总结该用户喜欢的类型或话题。");
        return sb.toString();
    }

    private String buildRulePreferenceSummary(BehaviorSnapshot snapshot) {
        List<String> cats = new ArrayList<>();
        for (String t : snapshot.topCategories()) {
            int idx = t.indexOf('（');
            cats.add(idx > 0 ? t.substring(0, idx) : t);
        }
        if (!cats.isEmpty()) {
            return "您喜欢的类型是" + String.join("、", cats) + "。";
        }
        if (!snapshot.subCategories().isEmpty()) {
            return "您订阅了" + String.join("、", snapshot.subCategories()) + " 等分类内容。";
        }
        if (snapshot.actionCount() == 0) {
            return "您暂未点赞或收藏帖子，以下按论坛热门内容为您推荐。";
        }
        return "您近期的互动偏好尚不明显，以下结合热门内容为您推荐。";
    }

    private List<Post> buildCandidatePosts(Long userId, Set<Long> excludeIds) {
        QueryWrapper<Post> wrapper = publishedWrapper();
        wrapper.ne("forum_post.user_id", userId);
        if (!excludeIds.isEmpty()) {
            wrapper.notIn("forum_post.id", excludeIds);
        }
        wrapper.orderByDesc("forum_post.like_count")
                .orderByDesc("forum_post.view_count")
                .orderByDesc("forum_post.create_time")
                .last("LIMIT " + CANDIDATE_LIMIT);
        return postMapper.selectListWithNickname(wrapper);
    }

    private AiRecommend callAiRecommend(BehaviorSnapshot snapshot, List<Post> candidates) {
        Map<Long, Category> categoryMap = categoryService.list().stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        String candidatesJson = candidates.stream()
                .map(p -> candidateJson(p, categoryMap))
                .collect(Collectors.joining(",", "[", "]"));

        String userMsg = "【用户行为】\n"
                + "- 点赞帖子（" + snapshot.likedLines().size() + "条）：\n"
                + (snapshot.likedLines().isEmpty() ? "  （无）\n" : snapshot.likedLines().stream().map(s -> "  · " + s).collect(Collectors.joining("\n")) + "\n")
                + "- 收藏帖子（" + snapshot.collectedLines().size() + "条）：\n"
                + (snapshot.collectedLines().isEmpty() ? "  （无）\n" : snapshot.collectedLines().stream().map(s -> "  · " + s).collect(Collectors.joining("\n")) + "\n")
                + "- 订阅分类：" + (snapshot.subCategories().isEmpty() ? "无" : String.join("、", snapshot.subCategories())) + "\n"
                + "- 关注用户：" + (snapshot.followNames().isEmpty() ? "无" : String.join("、", snapshot.followNames())) + "\n\n"
                + "【候选帖子 JSON】\n" + candidatesJson + "\n\n"
                + "请从候选帖子中选出 " + MIN_RECOMMEND + "～" + MAX_RECOMMEND + " 篇最可能感兴趣的帖子。\n"
                + "只输出 JSON：\n"
                + "{\n"
                + "  \"postIds\": [按推荐优先级排序的帖子 id 数字数组]\n"
                + "}";

        // 使用 Tool Calling 方式
        List<Message> messages = Arrays.asList(
                new Message("system", "你是论坛个性化推荐助手，请调用 recommend_posts 工具返回推荐结果。"),
                new Message("user", userMsg)
        );

        try {
            ChatResponse resp = aiService.getCompletionWithTools(
                    messages,
                    List.of(com.zhidao.demo.util.AiTools.recommendPostsTool())
            ).block();

            Message msg = resp.getChoices().get(0).getMessage();
            if (msg.getTool_calls() != null && !msg.getTool_calls().isEmpty()) {
                String args = msg.getTool_calls().get(0).getFunction().getArguments();
                return parseAiRecommend(args); // 复用解析逻辑
            }
            return fallbackFromCandidates(candidates);
        } catch (Exception e) {
            log.error("AI 推荐失败", e);
            return fallbackFromCandidates(candidates);
        }
    }

    private AiRecommend parseAiRecommend(String raw) {
        if (raw == null || raw.isBlank()) {
            return new AiRecommend(List.of(), "");
        }
        String json = raw.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").trim();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            List<Long> ids = new ArrayList<>();
            if (node.has("postIds") && node.get("postIds").isArray()) {
                for (JsonNode idNode : node.get("postIds")) {
                    if (idNode.isNumber()) ids.add(idNode.longValue());
                    else if (idNode.isTextual()) {
                        try {
                            ids.add(Long.parseLong(idNode.asText().trim()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            String summary = "";
            if (node.has("preferenceSummary")) summary = node.get("preferenceSummary").asText("");
            else if (node.has("summary")) summary = node.get("summary").asText("");
            return new AiRecommend(ids, summary);
        } catch (Exception e) {
            log.warn("解析 AI 推荐 JSON 失败: {}", raw, e);
            return new AiRecommend(List.of(), "");
        }
    }

    private List<Long> validateAndOrderIds(List<Long> aiIds, List<Post> candidates) {
        Set<Long> allowed = candidates.stream().map(Post::getId).collect(Collectors.toSet());
        List<Long> result = new ArrayList<>();
        for (Long id : aiIds) {
            if (id != null && allowed.contains(id) && !result.contains(id)) {
                result.add(id);
            }
            if (result.size() >= MAX_RECOMMEND) break;
        }
        return result;
    }

    private List<Long> padFromCandidates(List<Long> current, List<Post> candidates, int minSize) {
        List<Long> result = new ArrayList<>(current);
        for (Post p : candidates) {
            if (result.size() >= MAX_RECOMMEND) break;
            if (!result.contains(p.getId())) {
                result.add(p.getId());
            }
        }
        return result;
    }

    private AiRecommend fallbackFromCandidates(List<Post> candidates) {
        List<Long> ids = candidates.stream()
                .limit(MAX_RECOMMEND)
                .map(Post::getId)
                .collect(Collectors.toList());
        return new AiRecommend(ids, "");
    }

    private List<Post> loadPostsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Post> loaded = postMapper.selectListWithNickname(new QueryWrapper<Post>()
                .in("forum_post.id", ids)
                .eq("forum_post.is_deleted", 0));
        Map<Long, Post> map = loaded.stream().collect(Collectors.toMap(Post::getId, p -> p, (a, b) -> a));
        List<Post> ordered = new ArrayList<>();
        for (Long id : ids) {
            Post p = map.get(id);
            if (p != null) ordered.add(p);
        }
        return enrichAuthorProfile(ordered);
    }

    /** 规则降级（AI 完全失败且无缓存时备用，当前 refresh 路径用候选填充） */
    @SuppressWarnings("unused")
    private List<Post> fallbackRuleBased(User user) {
        List<UserAction> likedActions = userActionMapper.selectList(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, user.getId())
                .eq(UserAction::getType, UserAction.TYPE_LIKE_POST));
        List<Long> likedPostIds = likedActions.stream().map(UserAction::getTargetId).collect(Collectors.toList());

        QueryWrapper<Post> wrapper = publishedWrapper();
        wrapper.ne("forum_post.user_id", user.getId());
        if (!likedPostIds.isEmpty()) {
            List<Post> likedPosts = postMapper.selectBatchIds(likedPostIds);
            List<Long> likedCategoryIds = likedPosts.stream().map(Post::getCategoryId).distinct().collect(Collectors.toList());
            if (!likedCategoryIds.isEmpty()) {
                wrapper.in("forum_post.category_id", likedCategoryIds)
                        .notIn("forum_post.id", likedPostIds);
            }
        }
        wrapper.orderByDesc("forum_post.create_time").last("LIMIT " + MAX_RECOMMEND);
        return enrichAuthorProfile(postMapper.selectListWithNickname(wrapper));
    }

    private List<Post> enrichAuthorProfile(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return posts == null ? Collections.emptyList() : posts;
        }
        List<Long> userIds = posts.stream().map(Post::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        for (Post post : posts) {
            User author = userMap.get(post.getUserId());
            if (author == null) continue;
            if (post.getUserNickname() == null || post.getUserNickname().isBlank()) {
                post.setUserNickname(author.getNickname());
            }
            if (post.getUserAvatar() == null || post.getUserAvatar().isBlank()) {
                post.setUserAvatar(author.getAvatar());
            }
        }
        return posts;
    }

    private static QueryWrapper<Post> publishedWrapper() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("forum_post.status", "PUBLISHED");
        wrapper.eq("forum_post.is_deleted", 0);
        return wrapper;
    }

    private String categoryName(Map<Long, Category> map, Long categoryId) {
        if (categoryId == null) return "";
        Category c = map.get(categoryId);
        return c != null && c.getName() != null ? c.getName() : "";
    }

    private String formatPostLine(Post post, String categoryName) {
        String excerpt = AiTextUtils.prepareContext(post.getContent(), EXCERPT_CHARS);
        return String.format(Locale.ROOT, "《%s》[%s] %s",
                truncate(post.getTitle(), 40),
                categoryName.isBlank() ? "未分类" : categoryName,
                excerpt);
    }

    private String candidateJson(Post p, Map<Long, Category> categoryMap) {
        String excerpt = AiTextUtils.prepareContext(p.getContent(), EXCERPT_CHARS);
        String cat = categoryName(categoryMap, p.getCategoryId());
        return String.format(Locale.ROOT,
                "{\"id\":%d,\"title\":%s,\"category\":%s,\"excerpt\":%s,\"likeCount\":%d,\"viewCount\":%d}",
                p.getId(),
                toJsonString(p.getTitle()),
                toJsonString(cat),
                toJsonString(excerpt),
                p.getLikeCount() != null ? p.getLikeCount() : 0,
                p.getViewCount() != null ? p.getViewCount() : 0);
    }

    private static String toJsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }

    private record BehaviorSnapshot(
            int actionCount,
            Set<Long> interactedPostIds,
            List<String> likedLines,
            List<String> collectedLines,
            List<String> subCategories,
            List<String> followNames,
            List<String> topCategories
    ) {}

    private record AiRecommend(List<Long> postIds, String summary) {}
}
