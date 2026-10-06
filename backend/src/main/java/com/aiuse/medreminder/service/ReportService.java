package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.mapper.ElderMapper;
import com.aiuse.medreminder.mapper.MedicationLogMapper;
import com.aiuse.medreminder.mapper.MedicationPlanMapper;
import com.aiuse.medreminder.security.CurrentUserHolder;
import com.aiuse.medreminder.vo.AdherenceReport;
import com.aiuse.medreminder.vo.DailyAdherence;
import com.aiuse.medreminder.vo.DrugAdherence;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服药依从性报告：周报 / 月报。
 *
 * <p>应服次数来自"进行中且当天有效的计划 × 提醒时间点"，已服次数来自打卡记录，
 * 两者之差即漏服次数。</p>
 */
@Service
public class ReportService {

    private static final int MAX_DAYS = 92;

    private final MedicationPlanMapper planMapper;
    private final MedicationLogMapper logMapper;
    private final ElderMapper elderMapper;

    public ReportService(MedicationPlanMapper planMapper, MedicationLogMapper logMapper,
                         ElderMapper elderMapper) {
        this.planMapper = planMapper;
        this.logMapper = logMapper;
        this.elderMapper = elderMapper;
    }

    public AdherenceReport report(long elderId, String period, LocalDate anchorDate) {
        long ownerId = CurrentUserHolder.requireUserId();
        Elder elder = elderMapper.findById(elderId, ownerId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));

        LocalDate anchor = anchorDate == null ? LocalDate.now() : anchorDate;
        LocalDate from;
        LocalDate to;
        boolean monthly = "month".equalsIgnoreCase(period);
        if (monthly) {
            from = anchor.withDayOfMonth(1);
            to = anchor.withDayOfMonth(anchor.lengthOfMonth());
        } else {
            LocalDate monday = anchor.minusDays(anchor.getDayOfWeek().getValue() - 1L);
            from = monday;
            to = monday.plusDays(6);
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw BizException.badRequest("统计区间过长");
        }

        List<MedicationPlan> plans = planMapper.findActiveByElderId(elderId, ownerId);
        List<MedicationLog> logs = logMapper.findByElderIdAndDateRange(elderId, from, to, ownerId);

        // 每天已服次数
        Map<LocalDate, Integer> takenByDate = new HashMap<>();
        Map<Long, Integer> takenByPlan = new HashMap<>();
        for (MedicationLog log : logs) {
            if ("TAKEN".equals(log.getStatus())) {
                takenByDate.merge(log.getTakeDate(), 1, Integer::sum);
                takenByPlan.merge(log.getPlanId(), 1, Integer::sum);
            }
        }

        List<DailyAdherence> daily = new ArrayList<>();
        int totalExpected = 0;
        int totalTaken = 0;
        int missedDays = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            int expected = 0;
            for (MedicationPlan plan : plans) {
                if (MedicationPlanService.planValidOn(plan, day)) {
                    expected += MedicationPlanService.remindTimesOf(plan).size();
                }
            }
            int taken = Math.min(takenByDate.getOrDefault(day, 0), expected);
            totalExpected += expected;
            totalTaken += taken;
            if (expected > 0 && taken < expected) {
                missedDays++;
            }
            daily.add(new DailyAdherence(day, expected, taken, Math.max(0, expected - taken)));
        }

        List<DrugAdherence> byDrug = new ArrayList<>();
        for (MedicationPlan plan : plans) {
            int expected = 0;
            for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
                if (MedicationPlanService.planValidOn(plan, day)) {
                    expected += MedicationPlanService.remindTimesOf(plan).size();
                }
            }
            if (expected == 0) {
                continue;
            }
            int taken = Math.min(takenByPlan.getOrDefault(plan.getId(), 0), expected);
            byDrug.add(new DrugAdherence(plan.getId(), plan.getDrugName(), plan.getDosage(),
                    expected, taken, expected - taken, rate(taken, expected)));
        }

        int missed = Math.max(0, totalExpected - totalTaken);
        String periodName = monthly ? "本月" : "本周";
        String summary = String.format("%s应服 %d 次，实服 %d 次，漏服 %d 次，依从性 %.1f%%",
                periodName, totalExpected, totalTaken, missed, rate(totalTaken, totalExpected) * 100);

        return new AdherenceReport(monthly ? "month" : "week", from, to, elder.getId(), elder.getName(),
                totalExpected, totalTaken, missed, rate(totalTaken, totalExpected), missedDays,
                summary, daily, byDrug);
    }

    private static double rate(int taken, int expected) {
        if (expected <= 0) {
            return 0d;
        }
        return Math.round(taken * 1000.0 / expected) / 1000.0;
    }
}

