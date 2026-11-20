package com.cqust.repair.module.ai.service.impl;

import com.cqust.repair.config.AppProperties;
import com.cqust.repair.module.ai.dto.AiLinkVO;
import com.cqust.repair.module.ai.service.KnowledgeResult;
import com.cqust.repair.module.ai.service.KnowledgeService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库实现：启动加载 Markdown，按标题切片，二元词检索。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Pattern HEADING = Pattern.compile("^#{2,3}\\s+(.+)$");
    private static final Pattern META = Pattern.compile("^<!--\\s*meta:(.*?)-->$");
    private static final Pattern ENGLISH = Pattern.compile("[a-z0-9]+");
    private static final Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Set<String> VALID_ROLES =
            Set.of("COMMON", "STUDENT", "REPAIRER", "MANAGER");

    private final AppProperties appProperties;
    private final List<Chunk> chunks = new ArrayList<>();

    @PostConstruct
    public void load() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:ai/knowledge/*.md");
            Arrays.sort(resources, Comparator.comparing(item -> {
                String name = item.getFilename();
                return name == null ? "" : name;
            }));
            for (Resource resource : resources) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                parse(resource.getFilename(), content);
            }
            log.info("AI 知识库加载完成，片段数={}", chunks.size());
        } catch (Exception e) {
            log.error("AI 知识库加载失败: {}", e.getMessage());
        }
    }

    @Override
    public KnowledgeResult search(String question, String role) {
        if (chunks.isEmpty()) {
            return KnowledgeResult.empty();
        }
        String currentRole = role == null ? "COMMON" : role.toUpperCase(Locale.ROOT);
        Set<String> tokens = tokenize(question);
        List<ScoredChunk> scored = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (!chunk.roles.contains("COMMON") && !chunk.roles.contains(currentRole)) {
                continue;
            }
            double score = 0;
            for (String token : tokens) {
                score += count(chunk.title, token) * 3.0;
                score += count(chunk.body, token);
            }
            if (chunk.roles.contains(currentRole)) {
                score += 0.5;
            }
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());

        AppProperties.Ai.Knowledge knowledge = appProperties.getAi().getKnowledge();
        int topK = Math.max(1, knowledge.getTopK());
        List<Chunk> selected = scored.stream()
                .filter(item -> item.getScore() >= knowledge.getMinScore())
                .limit(topK)
                .map(ScoredChunk::getChunk)
                .toList();
        if (selected.isEmpty()) {
            selected = fallback(currentRole, topK);
        }
        return buildResult(selected, knowledge.getMaxChars());
    }

    private void parse(String file, String text) {
        String title = null;
        StringBuilder body = new StringBuilder();
        Set<String> roles = new HashSet<>();
        String link = null;
        String label = null;
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                flush(file, title, body, roles, link, label);
                title = heading.group(1).trim();
                body = new StringBuilder();
                roles = new HashSet<>();
                link = null;
                label = null;
                continue;
            }
            Matcher meta = META.matcher(line);
            if (meta.matches() && title != null) {
                for (String part : meta.group(1).split(";")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length < 2) {
                        continue;
                    }
                    String key = kv[0].trim().toLowerCase(Locale.ROOT);
                    String value = kv[1].trim();
                    if ("roles".equals(key)) {
                        roles.clear();
                        for (String role : value.split("[,|]")) {
                            String normalized = role.trim().toUpperCase(Locale.ROOT);
                            if (VALID_ROLES.contains(normalized)) {
                                roles.add(normalized);
                            }
                        }
                    } else if ("link".equals(key)) {
                        link = value;
                    } else if ("label".equals(key)) {
                        label = value;
                    }
                }
                continue;
            }
            if (title != null) {
                body.append(raw).append('\n');
            }
        }
        flush(file, title, body, roles, link, label);
    }

    private void flush(String file, String title, StringBuilder body,
                       Set<String> roles, String link, String label) {
        if (!StringUtils.hasText(title)) {
            return;
        }
        String content = body.toString().trim();
        if (!StringUtils.hasText(content)) {
            return;
        }
        Set<String> finalRoles = roles.isEmpty() ? Set.of("COMMON") : new HashSet<>(roles);
        chunks.add(new Chunk(title, content, finalRoles, link, label, file));
    }

    private List<Chunk> fallback(String role, int topK) {
        List<Chunk> result = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (chunk.roles.contains("COMMON") || chunk.roles.contains(role)) {
                result.add(chunk);
                if (result.size() >= topK) {
                    break;
                }
            }
        }
        return result;
    }

    private KnowledgeResult buildResult(List<Chunk> selected, int maxChars) {
        if (selected.isEmpty()) {
            return KnowledgeResult.empty();
        }
        StringBuilder context = new StringBuilder();
        List<AiLinkVO> links = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        for (Chunk chunk : selected) {
            if (StringUtils.hasText(chunk.link) && StringUtils.hasText(chunk.label)
                    && chunk.link.startsWith("/") && paths.add(chunk.link) && links.size() < 3) {
                links.add(new AiLinkVO(chunk.label, chunk.link));
            }
            String block = "## " + chunk.title + "\n" + chunk.body + "\n\n";
            if (context.length() + block.length() > maxChars) {
                int remain = maxChars - context.length();
                if (remain > 20) {
                    context.append(block, 0, remain);
                }
                break;
            }
            context.append(block);
        }
        return new KnowledgeResult(context.toString().trim(), links);
    }

    private Set<String> tokenize(String question) {
        Set<String> tokens = new HashSet<>();
        if (!StringUtils.hasText(question)) {
            return tokens;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        Matcher english = ENGLISH.matcher(lower);
        while (english.find()) {
            tokens.add(english.group());
        }
        Matcher chinese = CHINESE.matcher(lower);
        while (chinese.find()) {
            String text = chinese.group();
            if (text.length() == 1) {
                tokens.add(text);
            }
            for (int i = 0; i + 1 < text.length(); i++) {
                tokens.add(text.substring(i, i + 2));
            }
        }
        return tokens;
    }

    private int count(String text, String token) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(token)) {
            return 0;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int count = 0;
        int index = 0;
        while ((index = lower.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    @Data
    @AllArgsConstructor
    private static class Chunk {
        private String title;
        private String body;
        private Set<String> roles;
        private String link;
        private String label;
        private String file;
    }

    @Data
    @AllArgsConstructor
    private static class ScoredChunk {
        private Chunk chunk;
        private double score;
    }
}
