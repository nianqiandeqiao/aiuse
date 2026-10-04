package com.aiuse.medreminder.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 药品别名词典：把 OCR 识别出的商品名/剂型名归一成标准药名。
 * 词典文件：resources/drug-dictionary.txt，新增药品只要加一行，不用改代码。
 */
@Component
public class DrugDictionary {

    private static final Logger log = LoggerFactory.getLogger(DrugDictionary.class);

    /** 一条药品：标准名、类别、全部可匹配关键词（标准名 + 别名）。 */
    public record DrugEntry(String standardName, String category, List<String> keywords) {
    }

    /** 命中结果：标准名、类别、命中的关键词。 */
    public record DrugHit(String standardName, String category, String matchedKeyword) {
    }

    private final List<DrugEntry> entries = new ArrayList<>();
    private final Map<String, DrugEntry> byKeyword = new LinkedHashMap<>();
    private final Map<String, DrugEntry> byStandardName = new LinkedHashMap<>();

    @PostConstruct
    void load() {
        ClassPathResource resource = new ClassPathResource("drug-dictionary.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\|");
                if (parts.length < 2) {
                    continue;
                }
                String standardName = parts[0].trim();
                List<String> keywords = new ArrayList<>();
                keywords.add(standardName);
                for (String alias : parts[1].split(",")) {
                    String aliasTrimmed = alias.trim();
                    if (!aliasTrimmed.isEmpty()) {
                        keywords.add(aliasTrimmed);
                    }
                }
                String category = parts.length > 2 ? parts[2].trim() : "";
                DrugEntry entry = new DrugEntry(standardName, category, keywords);
                entries.add(entry);
                byStandardName.put(standardName, entry);
                for (String keyword : keywords) {
                    byKeyword.putIfAbsent(normalize(keyword), entry);
                }
            }
        } catch (Exception e) {
            log.error("加载药品词典失败", e);
        }
        // 长关键词优先匹配，避免"阿司匹林"抢先命中"阿司匹林肠溶片"里的片段
        entries.sort(Comparator.comparingInt((DrugEntry entry) -> longestKeyword(entry)).reversed());
        log.info("药品词典加载完成，共 {} 条药品、{} 个关键词", entries.size(), byKeyword.size());
    }

    /** 从一整段 OCR 文本里找出所有已知药品。 */
    public List<DrugHit> match(String text) {
        List<DrugHit> hits = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return hits;
        }
        String normalizedText = normalize(text);
        Map<String, DrugHit> deduped = new LinkedHashMap<>();
        for (DrugEntry entry : entries) {
            for (String keyword : entry.keywords()) {
                if (normalizedText.contains(normalize(keyword))) {
                    deduped.putIfAbsent(entry.standardName(),
                            new DrugHit(entry.standardName(), entry.category(), keyword));
                    break;
                }
            }
        }
        hits.addAll(deduped.values());
        return hits;
    }

    /** 把单个药名归一成标准名；词典里没有的返回原名（去掉首尾空白）。 */
    public String standardize(String drugName) {
        if (drugName == null) {
            return null;
        }
        String normalized = normalize(drugName);
        DrugEntry exact = byKeyword.get(normalized);
        if (exact != null) {
            return exact.standardName();
        }
        for (Map.Entry<String, DrugEntry> entry : byKeyword.entrySet()) {
            if (!entry.getKey().isEmpty() && normalized.contains(entry.getKey())) {
                return entry.getValue().standardName();
            }
        }
        return drugName.trim();
    }

    public Optional<DrugEntry> findByStandardName(String standardName) {
        return Optional.ofNullable(byStandardName.get(standardName));
    }

    public int size() {
        return entries.size();
    }

    /** 归一化：去掉空白与常见分隔符，全角字符转半角，便于匹配。 */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c) || c == '　') {
                continue;
            }
            if (c >= 'Ａ' && c <= 'Ｚ') {
                sb.append((char) (c - 0xFEE0));
            } else if (c >= 'ａ' && c <= 'ｚ') {
                sb.append((char) (c - 0xFEE0));
            } else if (c >= '０' && c <= '９') {
                sb.append((char) (c - 0xFEE0));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int longestKeyword(DrugEntry entry) {
        return entry.keywords().stream().mapToInt(String::length).max().orElse(0);
    }
}

