package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.mapper.ElderMapper;
import com.aiuse.medreminder.mapper.MedicationPlanMapper;
import com.aiuse.medreminder.security.CurrentUserHolder;
import com.aiuse.medreminder.service.DrugDictionary.DrugHit;
import com.aiuse.medreminder.vo.AskAnswer;
import com.aiuse.medreminder.vo.InteractionCheckResult;
import com.aiuse.medreminder.vo.InteractionConflict;
import com.aiuse.medreminder.vo.MedicineAnalysis;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用药智能助手。
 *
 * <p>优先调用大模型（OpenAI 兼容接口）；没有配置密钥或调用失败时，降级为内置规则引擎，
 * 用药品词典和相互作用规则库给出有依据的回答，保证没有外部依赖也能演示且不编造。</p>
 */
@Service
public class MedicationAssistantService {

    /** 河南话常用词归一，让语音提问更贴近老人的说法。 */
    private static final Map<String, String> HENAN_WORDS = new LinkedHashMap<>();

    static {
        HENAN_WORDS.put("喝药", "吃药");
        HENAN_WORDS.put("恁", "你");
        HENAN_WORDS.put("中不中", "行不行");
        HENAN_WORDS.put("管不管", "行不行");
        HENAN_WORDS.put("啥", "什么");
        HENAN_WORDS.put("咋", "怎么");
        HENAN_WORDS.put("木有", "没有");
        HENAN_WORDS.put("夜儿个", "昨天");
        HENAN_WORDS.put("今儿个", "今天");
        HENAN_WORDS.put("明儿个", "明天");
        HENAN_WORDS.put("后晌", "晚上");
        HENAN_WORDS.put("晌午", "中午");
        HENAN_WORDS.put("一搭", "一起");
        HENAN_WORDS.put("该咋", "该怎么");
    }

    private static final String DISCLAIMER = "（本回答仅作用药提醒，具体请遵医嘱或咨询药师）";

    private static final Pattern[] DATE_PATTERNS = {
            Pattern.compile("(20\\d{2})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})?\\s*日?"),
            Pattern.compile("(20\\d{2})[-/.](\\d{1,2})[-/.]?(\\d{1,2})?"),
            Pattern.compile("(20\\d{2})(\\d{2})(\\d{2})")
    };

    private final ElderMapper elderMapper;
    private final MedicationPlanMapper planMapper;
    private final InteractionService interactionService;
    private final DrugDictionary drugDictionary;
    private final LlmClient llmClient;

    public MedicationAssistantService(ElderMapper elderMapper, MedicationPlanMapper planMapper,
                                      InteractionService interactionService,
                                      DrugDictionary drugDictionary, LlmClient llmClient) {
        this.elderMapper = elderMapper;
        this.planMapper = planMapper;
        this.interactionService = interactionService;
        this.drugDictionary = drugDictionary;
        this.llmClient = llmClient;
    }

    /** 语音/文字问药。 */
    public AskAnswer ask(long elderId, String rawQuestion, String dialect) {
        long ownerId = CurrentUserHolder.requireUserId();
        Elder elder = elderMapper.findById(elderId, ownerId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));
        String question = normalizeDialect(rawQuestion, dialect);
        List<MedicationPlan> plans = planMapper.findActiveByElderId(elderId, ownerId);

        RuleResult rule = ruleAnswer(question, elder, plans);
        if (llmClient.isAvailable()) {
            String answer = llmClient.chat(buildSystemPrompt(elder, plans), question);
            if (answer != null && !answer.isBlank()) {
                return new AskAnswer(answer.trim(), AskAnswer.SOURCE_LLM, rule.references(),
                        rule.conflicts(), toSpoken(answer));
            }
        }
        return new AskAnswer(rule.answer(), AskAnswer.SOURCE_RULE, rule.references(),
                rule.conflicts(), rule.answer());
    }

    /** 药盒说明书分析：识别药品、判断是否过期、检查与在服药的冲突。 */
    public MedicineAnalysis analyzeMedicine(long elderId, String ocrText) {
        long ownerId = CurrentUserHolder.requireUserId();
        elderMapper.findById(elderId, ownerId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));

        List<DrugHit> drugs = drugDictionary.match(ocrText);
        ExpireInfo expire = extractExpire(ocrText);

        List<MedicationPlan> plans = planMapper.findActiveByElderId(elderId, ownerId);
        List<String> allDrugs = new ArrayList<>();
        plans.forEach(plan -> allDrugs.add(plan.getDrugName()));
        drugs.forEach(hit -> allDrugs.add(hit.standardName()));
        List<InteractionConflict> conflicts = allDrugs.size() >= 2
                ? interactionService.checkDrugNames(allDrugs).conflicts()
                : List.of();

        List<String> advices = new ArrayList<>();
        if (expire.date() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), expire.date());
            if (days < 0) {
                advices.add("这盒药已经过期（有效期 " + expire.date() + "），请勿服用，建议交给药店回收处理。");
            } else if (days <= 30) {
                advices.add("这盒药还有 " + days + " 天到期（有效期 " + expire.date()
                        + "），建议尽快服用或者更换新药。");
            } else {
                advices.add("有效期到 " + expire.date() + "，目前没有过期。");
            }
        } else {
            advices.add("照片里没看清有效期，建议再拍一下药盒上标注的有效期或者生产日期。");
        }
        if (!conflicts.isEmpty()) {
            InteractionConflict first = conflicts.get(0);
            advices.add("检测到 " + conflicts.size() + " 条用药冲突，最严重的是「" + first.drugA() + " 和 "
                    + first.drugB() + "：" + first.title() + "」。");
        }
        for (DrugHit hit : drugs) {
            advices.add(hit.standardName() + " 属于" + hit.category() + "类药物，具体用法请按说明书或者医嘱。");
        }
        if (drugs.isEmpty()) {
            advices.add("没有识别出已知药品，建议把药名拍清楚一些，或者手动添加到用药计划。");
        }

        String summary = drugs.isEmpty()
                ? "未识别到药品"
                : "识别到 " + drugs.size() + " 种药品：" + String.join("、", drugs.stream().map(DrugHit::standardName).toList())
                        + (expire.date() != null ? "；有效期 " + expire.date() : "");

        String source = AskAnswer.SOURCE_RULE;
        if (llmClient.isAvailable()) {
            String llm = llmClient.chat(
                    "你是社区用药安全助手，请用老年人听得懂的短句回答，不要用专业术语，不确定时提醒咨询医生。",
                    "药盒说明文字：" + ocrText + "\n系统识别结果：" + summary
                            + "\n请用 3-5 句话说明：这药是干什么的、是否过期、能不能和老人正在吃的药一起吃。");
            if (llm != null && !llm.isBlank()) {
                advices.add(llm.trim());
                source = AskAnswer.SOURCE_LLM;
            }
        }
        return new MedicineAnalysis(drugs, expire.text(), expire.date(), expire.expired(),
                conflicts, advices, summary, source);
    }

    /* ---------------- 内部实现 ---------------- */

    private record RuleResult(String answer, List<String> references, List<InteractionConflict> conflicts) {
    }

    private record ExpireInfo(String text, LocalDate date, Boolean expired) {
    }

    private RuleResult ruleAnswer(String question, Elder elder, List<MedicationPlan> plans) {
        List<String> references = new ArrayList<>();
        List<String> activeDrugs = new ArrayList<>();
        plans.forEach(plan -> activeDrugs.add(plan.getDrugName()));

        if (containsAny(question, "一起", "同服", "同吃", "相克", "冲突", "同时吃")) {
            List<String> drugs = new ArrayList<>(activeDrugs);
            drugDictionary.match(question).forEach(hit -> drugs.add(hit.standardName()));
            if (drugs.size() < 2) {
                return new RuleResult("请告诉我您想一起吃的两种药名，我帮您查一下能不能同服。" + DISCLAIMER,
                        references, List.of());
            }
            InteractionCheckResult result = interactionService.checkDrugNames(drugs);
            StringBuilder sb = new StringBuilder();
            if (result.conflicts().isEmpty()) {
                sb.append("这几样药没有查到已知的冲突：").append(String.join("、", drugs)).append("。");
            } else {
                sb.append("查到 ").append(result.conflictCount()).append(" 条用药风险：");
                for (InteractionConflict conflict : result.conflicts()) {
                    sb.append("\n").append(conflict.drugA()).append(" 和 ").append(conflict.drugB())
                            .append("（").append(conflict.severityLabel()).append("）：")
                            .append(conflict.title()).append("。").append(conflict.advice());
                    references.add(conflict.source());
                }
            }
            sb.append(DISCLAIMER);
            return new RuleResult(sb.toString(), references, result.conflicts());
        }

        if (containsAny(question, "饭前", "饭后", "什么时候吃", "怎么吃", "几点吃", "空腹", "睡前")) {
            List<DrugHit> hits = drugDictionary.match(question);
            StringBuilder sb = new StringBuilder();
            if (hits.isEmpty()) {
                sb.append("您问的是哪种药？可以说：阿司匹林饭前吃还是饭后吃。");
            } else {
                String drug = hits.get(0).standardName();
                references.add("药品词典：" + drug + "（" + hits.get(0).category() + "）");
                MedicationPlan matched = null;
                for (MedicationPlan plan : plans) {
                    if (drugDictionary.standardize(plan.getDrugName()).equals(drug)) {
                        matched = plan;
                        break;
                    }
                }
                if (matched == null) {
                    sb.append(drug).append(" 现在不在用药计划里，建议按说明书服用。");
                } else {
                    sb.append(drug).append(" 建议").append(matched.getTakeRelation() == null
                                    ? "按说明书服用" : matched.getTakeRelation())
                            .append("，每天").append(matched.getFrequency() == null
                                    ? "按医嘱" : matched.getFrequency())
                            .append("，提醒时间是 ").append(matched.getRemindTimes()).append("。");
                }
            }
            sb.append(DISCLAIMER);
            return new RuleResult(sb.toString(), references, List.of());
        }

        if (containsAny(question, "过期", "有效期", "还能不能吃")) {
            StringBuilder sb = new StringBuilder();
            List<String> expired = new ArrayList<>();
            for (MedicationPlan plan : plans) {
                if (plan.getExpireDate() != null && plan.getExpireDate().isBefore(LocalDate.now())) {
                    expired.add(plan.getDrugName() + "（" + plan.getExpireDate() + " 到期）");
                }
            }
            if (expired.isEmpty()) {
                sb.append(elder.getName()).append(" 现在登记的药都没有过期。");
            } else {
                sb.append("有 ").append(expired.size()).append(" 种药已经过期，不能再吃：")
                        .append(String.join("、", expired)).append("。");
            }
            sb.append(DISCLAIMER);
            return new RuleResult(sb.toString(), references, List.of());
        }

        if (containsAny(question, "忘了", "漏", "没吃", "补吃", "错过")) {
            return new RuleResult("刚想起来可以马上补服；如果快到下一次吃药时间了，这次就跳过，"
                    + "千万不要一次吃两倍的量。经常漏服要让家里人或者医生知道。" + DISCLAIMER,
                    references, List.of());
        }

        String answer = activeDrugs.isEmpty()
                ? "现在还没有登记用药，可以先拍一下药盒，或者让家里人帮忙把药录进来。"
                : elder.getName() + " 现在登记的药有：" + String.join("、", activeDrugs)
                        + "。您可以问我：这个药能和感冒药一起吃吗？饭前吃还是饭后吃？这个药过期了吗？";
        return new RuleResult(answer + DISCLAIMER, references, List.of());
    }

    private String buildSystemPrompt(Elder elder, List<MedicationPlan> plans) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是社区用药安全助手，服务对象是老年人和他们的家属。\n");
        sb.append("要求：用短句、口语化、不带专业术语；不确定时明确提醒咨询医生或药师；不要编造药品信息；回答控制在 4 句以内。\n");
        sb.append("老人：").append(elder.getName()).append("，年龄 ")
                .append(elder.getAge() == null ? "未填" : elder.getAge()).append("，基础疾病：")
                .append(elder.getChronicDisease() == null ? "未填" : elder.getChronicDisease()).append("\n");
        sb.append("当前在服药物：\n");
        for (MedicationPlan plan : plans) {
            sb.append("- ").append(plan.getDrugName()).append("，剂量 ").append(plan.getDosage())
                    .append("，").append(plan.getTakeRelation() == null ? "时机未填" : plan.getTakeRelation())
                    .append("，提醒时间 ").append(plan.getRemindTimes())
                    .append(plan.getExpireDate() == null ? "" : "，有效期 " + plan.getExpireDate())
                    .append("\n");
        }
        sb.append("回答最后必须附上：").append(DISCLAIMER);
        return sb.toString();
    }

    /** 从说明书文字里找有效期：只取关键词附近的第一个日期，够用且不容易误判。 */
    private ExpireInfo extractExpire(String text) {
        if (text == null || text.isBlank()) {
            return new ExpireInfo(null, null, null);
        }
        String compact = text.replace(" ", "");
        int keywordIndex = -1;
        for (String keyword : new String[]{"有效期至", "效期至", "有效期", "有效日期"}) {
            int index = compact.indexOf(keyword);
            if (index >= 0 && (keywordIndex < 0 || index < keywordIndex)) {
                keywordIndex = index;
            }
        }
        String scope = keywordIndex >= 0 ? compact.substring(keywordIndex) : compact;
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(scope);
            if (!matcher.find()) {
                continue;
            }
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                String dayGroup = matcher.groupCount() >= 3 ? matcher.group(3) : null;
                int day = dayGroup == null || dayGroup.isEmpty() ? 1 : Integer.parseInt(dayGroup);
                if (month < 1 || month > 12 || day < 1 || day > 31) {
                    continue;
                }
                LocalDate date = LocalDate.of(year, month, day);
                return new ExpireInfo(matcher.group(), date, date.isBefore(LocalDate.now()));
            } catch (RuntimeException ignored) {
                // 解析失败继续找下一个
            }
        }
        return new ExpireInfo(null, null, null);
    }

    private String normalizeDialect(String question, String dialect) {
        if (question == null) {
            return "";
        }
        boolean henan = dialect == null || dialect.isBlank() || "henan".equalsIgnoreCase(dialect);
        if (!henan) {
            return question;
        }
        String result = question;
        for (Map.Entry<String, String> entry : HENAN_WORDS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String toSpoken(String answer) {
        return answer.replaceAll("[\\r\\n]+", "，").replace("**", "");
    }
}

