package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.entity.InteractionRule;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.mapper.ElderMapper;
import com.aiuse.medreminder.mapper.InteractionRuleMapper;
import com.aiuse.medreminder.mapper.MedicationPlanMapper;
import com.aiuse.medreminder.vo.InteractionCheckResult;
import com.aiuse.medreminder.vo.InteractionConflict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用药安全核心服务：相互作用检查 + 重复用药检查。
 *
 * <p>检查思路：先把药名（含商品名、剂型）用词典归一到标准名，
 * 再拿标准名两两组合去规则库匹配；此外单独检查同一老人的重复用药。</p>
 */
@Service
public class InteractionService {

    private final InteractionRuleMapper ruleMapper;
    private final MedicationPlanMapper planMapper;
    private final ElderMapper elderMapper;
    private final DrugDictionary drugDictionary;

    public InteractionService(InteractionRuleMapper ruleMapper, MedicationPlanMapper planMapper,
                              ElderMapper elderMapper, DrugDictionary drugDictionary) {
        this.ruleMapper = ruleMapper;
        this.planMapper = planMapper;
        this.elderMapper = elderMapper;
        this.drugDictionary = drugDictionary;
    }

    /** 检查某位老人全部"进行中"用药计划之间的冲突。 */
    public InteractionCheckResult checkByElder(long elderId) {
        elderMapper.findById(elderId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));
        List<String> drugs = planMapper.findActiveByElderId(elderId).stream()
                .map(MedicationPlan::getDrugName)
                .filter(name -> name != null && !name.isBlank())
                .map(drugDictionary::standardize)
                .toList();
        return doCheck(elderId, drugs);
    }

    /** 直接检查一组药名，用于开药前预检。 */
    public InteractionCheckResult checkDrugNames(List<String> rawDrugNames) {
        List<String> drugs = rawDrugNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .map(drugDictionary::standardize)
                .toList();
        return doCheck(null, drugs);
    }

    /** 规则库列表，答辩演示用。 */
    public List<InteractionRule> listRules() {
        return ruleMapper.findAll();
    }

    private InteractionCheckResult doCheck(Long elderId, List<String> drugs) {
        List<InteractionConflict> conflicts = new ArrayList<>();
        conflicts.addAll(findDuplicates(drugs));
        conflicts.addAll(findInteractions(drugs));
        conflicts.sort(Comparator.comparingInt(InteractionConflict::severity));
        return InteractionCheckResult.of(elderId, drugs, conflicts);
    }

    /** 重复用药检查：同一位老人的用药里同一个标准药名出现多次。 */
    private List<InteractionConflict> findDuplicates(List<String> drugs) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String drug : drugs) {
            counts.merge(drug, 1, Integer::sum);
        }
        List<InteractionConflict> conflicts = new ArrayList<>();
        counts.forEach((drug, count) -> {
            if (count > 1) {
                conflicts.add(new InteractionConflict(
                        InteractionConflict.TYPE_DUPLICATE,
                        drug,
                        drug,
                        2,
                        InteractionConflict.severityLabel(2),
                        "重复用药：检测到 " + drug + " 重复 " + count + " 次",
                        "同一位老人的用药计划中出现 " + count + " 次" + drug + "，可能存在重复开药或重复录入。",
                        "请核对是否重复开药；如为不同剂型或不同规格，建议咨询医生或药师确认。",
                        "系统规则（重复用药检测）"));
            }
        });
        return conflicts;
    }

    /** 两两组合去规则库匹配。 */
    private List<InteractionConflict> findInteractions(List<String> drugs) {
        List<InteractionRule> rules = ruleMapper.findAllEnabled();
        List<InteractionConflict> conflicts = new ArrayList<>();
        Set<String> firedRules = new HashSet<>();

        for (int i = 0; i < drugs.size(); i++) {
            for (int j = i + 1; j < drugs.size(); j++) {
                String first = drugs.get(i);
                String second = drugs.get(j);
                if (first.equals(second)) {
                    continue;
                }
                for (InteractionRule rule : rules) {
                    if (!rule.matches(first, second)) {
                        continue;
                    }
                    String key = rule.getId() + "|" + (first.compareTo(second) <= 0
                            ? first + "+" + second : second + "+" + first);
                    if (!firedRules.add(key)) {
                        continue;
                    }
                    conflicts.add(new InteractionConflict(
                            InteractionConflict.TYPE_INTERACTION,
                            rule.getDrugA(),
                            rule.getDrugB(),
                            rule.getSeverity(),
                            InteractionConflict.severityLabel(rule.getSeverity()),
                            rule.getTitle(),
                            rule.getDescription(),
                            rule.getAdvice(),
                            rule.getSource()));
                }
            }
        }
        return conflicts;
    }
}
