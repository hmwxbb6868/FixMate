package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.dto.user.SafetyAnswersDTO;
import com.fixmate.entity.SafetyRuleEntity;
import com.fixmate.mapper.SafetyRuleMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 高风险硬规则引擎：纯确定性，不调用任何模型。
 *
 * 命中来源取并集，五类同属最高紧急等级，不做严重度排序：
 * 1. 用户勾选的五项安全答案（任一为 true 即命中对应规则）；
 * 2. 文本关键词匹配（跳过否定表达，避免"没有漏电"误判）。
 */
@Service
public class SafetyRuleEngine {

    private static final Map<String, String> BOOLEAN_RULES = Map.of(
            "smokeOrBurningSmell", "SMOKE_FIRE",
            "electricLeakage", "ELECTRIC_LEAKAGE",
            "gasSmell", "GAS_LEAK",
            "suspectedRefrigerantLeak", "REFRIGERANT_LEAK",
            "severeWaterIngress", "SEVERE_WATER_INGRESS");

    private static final List<String> NEGATIONS = List.of("没有", "没", "无", "未", "不是");
    private static final String CLAUSE_BOUNDARIES = "，,。；;！!？?、\n\r：:";
    /** 否定词与目标之间的最大距离；超过则视为另一句话，宁可漏判否定也不能漏报。 */
    private static final int MAX_NEGATION_DISTANCE = 6;
    /** 转折词出现时，前面的否定不再作用于后面的风险词。 */
    private static final List<String> ADVERSATIVE_MARKERS = List.of("但", "不过", "可是", "然而", "却", "反而", "只是", "而是");
    /** 否定词与风险词之间只允许这些中性桥接词；出现别的内容就不能安全地判为否定。 */
    private static final List<String> NEGATION_BRIDGES = List.of(
            "任何", "明显", "一点", "丝毫", "出现", "发生", "看到", "发现", "闻到", "听到",
            "感觉", "觉得", "有", "过", "再", "还", "也", "都", "并", "且", "和", "或", "与", "及");

    private final SafetyRuleMapper safetyRuleMapper;
    private final ObjectMapper objectMapper;

    public SafetyRuleEngine(SafetyRuleMapper safetyRuleMapper, ObjectMapper objectMapper) {
        this.safetyRuleMapper = safetyRuleMapper;
        this.objectMapper = objectMapper;
    }

    public List<MatchedRule> match(SafetyAnswersDTO answers, String text) {
        List<SafetyRuleEntity> rules = enabledRules();
        Map<String, MatchedRule> matched = new LinkedHashMap<>();
        collectFromBooleans(answers, rules, matched);
        collectFromText(text, rules, matched);
        return new ArrayList<>(matched.values());
    }

    public List<MatchedRule> match(String text) {
        return match(null, text);
    }

    private void collectFromBooleans(SafetyAnswersDTO answers, List<SafetyRuleEntity> rules,
                                     Map<String, MatchedRule> matched) {
        if (answers == null) {
            return;
        }
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("smokeOrBurningSmell", answers.getSmokeOrBurningSmell());
        flags.put("electricLeakage", answers.getElectricLeakage());
        flags.put("gasSmell", answers.getGasSmell());
        flags.put("suspectedRefrigerantLeak", answers.getSuspectedRefrigerantLeak());
        flags.put("severeWaterIngress", answers.getSevereWaterIngress());
        for (Map.Entry<String, Boolean> entry : flags.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }
            String code = BOOLEAN_RULES.get(entry.getKey());
            rules.stream().filter(r -> r.getCode().equals(code)).findFirst()
                    .ifPresent(r -> matched.putIfAbsent(r.getCode(),
                            new MatchedRule(r.getCode(), r.getTitle(), r.getTemplateText())));
        }
    }

    private void collectFromText(String text, List<SafetyRuleEntity> rules, Map<String, MatchedRule> matched) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> allKeywords = rules.stream()
                .flatMap(r -> parseKeywords(r.getKeywords()).stream())
                .map(k -> k.toLowerCase(Locale.ROOT))
                .filter(k -> !k.isBlank())
                .distinct()
                .toList();
        for (SafetyRuleEntity rule : rules) {
            for (String keyword : parseKeywords(rule.getKeywords())) {
                String needle = keyword.toLowerCase(Locale.ROOT);
                if (!needle.isBlank() && containsAffirmed(normalized, needle, allKeywords)) {
                    matched.putIfAbsent(rule.getCode(),
                            new MatchedRule(rule.getCode(), rule.getTitle(), rule.getTemplateText()));
                    break;
                }
            }
        }
    }

    private boolean containsAffirmed(String text, String keyword, List<String> allKeywords) {
        int from = 0;
        while (true) {
            int idx = text.indexOf(keyword, from);
            if (idx < 0) {
                return false;
            }
            if (!isNegated(text, idx, allKeywords)) {
                return true;
            }
            from = idx + keyword.length();
        }
    }

    /**
     * 只有当否定词紧邻目标（同一分句内、中间没有夹带其它安全关键词）时才判定为否定。
     * 例如"没有冒烟但是有漏电"里，"没有"不能用来否定"漏电"，否则就是漏报。
     */
    private boolean isNegated(String text, int keywordStart, List<String> allKeywords) {
        int boundary = -1;
        for (int i = keywordStart - 1; i >= 0; i--) {
            if (CLAUSE_BOUNDARIES.indexOf(text.charAt(i)) >= 0) {
                boundary = i;
                break;
            }
        }
        String window = text.substring(boundary + 1, keywordStart);
        // 疑问形式（是不是漏电 / 有没有漏电 / 是否漏电）按可能命中处理，避免漏报
        if (window.endsWith("是不是") || window.endsWith("有没有") || window.endsWith("是否")) {
            return false;
        }

        int negIndex = -1;
        int negLength = 0;
        for (String negation : NEGATIONS) {
            int pos = window.lastIndexOf(negation);
            if (pos >= 0 && pos + negation.length() > negIndex + negLength) {
                negIndex = pos;
                negLength = negation.length();
            }
        }
        if (negIndex < 0) {
            return false;
        }
        String between = window.substring(negIndex + negLength);
        for (String marker : ADVERSATIVE_MARKERS) {
            if (between.contains(marker)) {
                // "没有异响但是漏电"：转折之后是新陈述，否定不能跨过去
                return false;
            }
        }
        for (String other : allKeywords) {
            if (between.contains(other)) {
                // 否定词与目标之间已经出现别的安全关键词，说明是并列的另一句陈述
                return false;
            }
        }
        if (between.length() > MAX_NEGATION_DISTANCE) {
            return false;
        }
        // 只允许"没有明显漏电""没闻到燃气味"这类中性桥接；其余内容一律按可能命中处理
        String residue = between;
        for (String bridge : NEGATION_BRIDGES) {
            residue = residue.replace(bridge, "");
        }
        return residue.isEmpty();
    }

    private List<SafetyRuleEntity> enabledRules() {
        return safetyRuleMapper.selectList(new LambdaQueryWrapper<SafetyRuleEntity>()
                        .eq(SafetyRuleEntity::getStatus, "ENABLED")
                        .orderByAsc(SafetyRuleEntity::getCode))
                .stream()
                .filter(r -> "ENABLED".equals(r.getStatus()))
                .toList();
    }

    private List<String> parseKeywords(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    public record MatchedRule(String ruleCode, String title, String templateText) {
    }
}
