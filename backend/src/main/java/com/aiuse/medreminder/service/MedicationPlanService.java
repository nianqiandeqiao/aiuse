package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.dto.MedicationPlanUpdateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.repository.ElderRepository;
import com.aiuse.medreminder.repository.MedicationLogRepository;
import com.aiuse.medreminder.repository.MedicationPlanRepository;
import com.aiuse.medreminder.vo.TodayDoseItem;
import com.aiuse.medreminder.vo.TodayDoseSummary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 用药计划：增删改查 + 今日服药清单。
 */
@Service
public class MedicationPlanService {

    /** 计划没有填写提醒时间时使用的默认时间。 */
    public static final String DEFAULT_REMIND_TIME = "08:00";

    private final MedicationPlanRepository planRepository;
    private final ElderRepository elderRepository;
    private final MedicationLogRepository logRepository;

    public MedicationPlanService(MedicationPlanRepository planRepository, ElderRepository elderRepository,
                                 MedicationLogRepository logRepository) {
        this.planRepository = planRepository;
        this.elderRepository = elderRepository;
        this.logRepository = logRepository;
    }

    public MedicationPlan create(MedicationPlanCreateRequest request) {
        elderRepository.findById(request.elderId())
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + request.elderId()));
        return planRepository.insert(request);
    }

    public List<MedicationPlan> listByElder(long elderId) {
        elderRepository.findById(elderId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));
        return planRepository.findByElderId(elderId);
    }

    public MedicationPlan get(long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("用药计划不存在: id=" + id));
    }

    public MedicationPlan update(long id, MedicationPlanUpdateRequest request) {
        int rows = planRepository.update(id, request);
        if (rows == 0) {
            throw BizException.notFound("用药计划不存在: id=" + id);
        }
        return get(id);
    }

    public MedicationPlan updateStatus(long id, int status) {
        if (status != 0 && status != 1) {
            throw BizException.badRequest("status 只能是 0(停用) 或 1(进行中)");
        }
        int rows = planRepository.updateStatus(id, status);
        if (rows == 0) {
            throw BizException.notFound("用药计划不存在: id=" + id);
        }
        return get(id);
    }

    public void delete(long id) {
        int rows = planRepository.softDelete(id);
        if (rows == 0) {
            throw BizException.notFound("用药计划不存在或已删除: id=" + id);
        }
    }

    /**
     * 今日服药清单：把老人所有"进行中"的计划展开成一个个提醒时间点，并标注是否已打卡。
     */
    public TodayDoseSummary today(long elderId, LocalDate date) {
        Elder elder = elderRepository.findById(elderId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));
        LocalDate target = date == null ? LocalDate.now() : date;

        List<TodayDoseItem> items = new ArrayList<>();
        for (MedicationPlan plan : planRepository.findActiveByElderId(elderId)) {
            if (!planValidOn(plan, target)) {
                continue;
            }
            for (String remindTime : remindTimesOf(plan)) {
                Optional<MedicationLog> log =
                        logRepository.findByPlanIdAndDateAndRemindTime(plan.id(), target, remindTime);
                items.add(new TodayDoseItem(
                        plan.id(),
                        plan.drugName(),
                        plan.dosage(),
                        plan.frequency(),
                        plan.takeRelation(),
                        remindTime,
                        log.isPresent(),
                        log.map(MedicationLog::id).orElse(null),
                        log.map(MedicationLog::status).orElse(null),
                        log.map(MedicationLog::takeTime).orElse(null)));
            }
        }
        items.sort(Comparator.comparing(TodayDoseItem::remindTime).thenComparing(TodayDoseItem::planId));

        int taken = (int) items.stream().filter(TodayDoseItem::taken).count();
        return new TodayDoseSummary(elderId, elder.name(), target, items.size(), taken,
                items.size() - taken, items);
    }

    /** 把 remindTimes 字符串（如 "08:00,20:00"）解析成时间点列表。 */
    public static List<String> remindTimesOf(MedicationPlan plan) {
        String raw = plan.remindTimes();
        if (raw == null || raw.isBlank()) {
            return List.of(DEFAULT_REMIND_TIME);
        }
        List<String> times = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(time -> !time.isEmpty())
                .distinct()
                .sorted()
                .toList();
        return times.isEmpty() ? List.of(DEFAULT_REMIND_TIME) : times;
    }

    /** 计划在指定日期是否生效（受开始/结束日期约束）。 */
    public static boolean planValidOn(MedicationPlan plan, LocalDate date) {
        if (plan.startDate() != null && date.isBefore(plan.startDate())) {
            return false;
        }
        return plan.endDate() == null || !date.isAfter(plan.endDate());
    }
}

