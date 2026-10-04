package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.dto.MedicationPlanUpdateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.mapper.ElderMapper;
import com.aiuse.medreminder.mapper.MedicationLogMapper;
import com.aiuse.medreminder.mapper.MedicationPlanMapper;
import com.aiuse.medreminder.security.CurrentUserHolder;
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
 * 用药计划业务层：增删改查 + 今日服药清单。
 * 计划归属通过 elder.owner_id 判断，只能操作自己老人的计划。
 */
@Service
public class MedicationPlanService {

    /** 计划没有填写提醒时间时使用的默认时间。 */
    public static final String DEFAULT_REMIND_TIME = "08:00";

    private final MedicationPlanMapper planMapper;
    private final ElderMapper elderMapper;
    private final MedicationLogMapper logMapper;

    public MedicationPlanService(MedicationPlanMapper planMapper, ElderMapper elderMapper,
                                 MedicationLogMapper logMapper) {
        this.planMapper = planMapper;
        this.elderMapper = elderMapper;
        this.logMapper = logMapper;
    }

    public MedicationPlan create(MedicationPlanCreateRequest request) {
        long ownerId = CurrentUserHolder.requireUserId();
        requireOwnedElder(request.elderId(), ownerId);
        MedicationPlan plan = new MedicationPlan();
        plan.setElderId(request.elderId());
        plan.setDrugName(request.drugName());
        plan.setDosage(request.dosage());
        plan.setFrequency(request.frequency());
        plan.setTakeRelation(request.takeRelation());
        plan.setRemindTimes(request.remindTimes());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        planMapper.insert(plan);
        return get(plan.getId());
    }

    public List<MedicationPlan> listByElder(long elderId) {
        long ownerId = CurrentUserHolder.requireUserId();
        requireOwnedElder(elderId, ownerId);
        return planMapper.findByElderId(elderId, ownerId);
    }

    public MedicationPlan get(long id) {
        long ownerId = CurrentUserHolder.requireUserId();
        return planMapper.findById(id, ownerId)
                .orElseThrow(() -> BizException.notFound("用药计划不存在: id=" + id));
    }

    public MedicationPlan update(long id, MedicationPlanUpdateRequest request) {
        long ownerId = CurrentUserHolder.requireUserId();
        MedicationPlan plan = new MedicationPlan();
        plan.setId(id);
        plan.setDrugName(request.drugName());
        plan.setDosage(request.dosage());
        plan.setFrequency(request.frequency());
        plan.setTakeRelation(request.takeRelation());
        plan.setRemindTimes(request.remindTimes());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        if (planMapper.update(plan, ownerId) == 0) {
            throw BizException.notFound("用药计划不存在: id=" + id);
        }
        return get(id);
    }

    public MedicationPlan updateStatus(long id, int status) {
        long ownerId = CurrentUserHolder.requireUserId();
        if (status != 0 && status != 1) {
            throw BizException.badRequest("status 只能是 0(停用) 或 1(进行中)");
        }
        if (planMapper.updateStatus(id, status, ownerId) == 0) {
            throw BizException.notFound("用药计划不存在: id=" + id);
        }
        return get(id);
    }

    public void delete(long id) {
        long ownerId = CurrentUserHolder.requireUserId();
        if (planMapper.softDelete(id, ownerId) == 0) {
            throw BizException.notFound("用药计划不存在或已删除: id=" + id);
        }
    }

    /**
     * 今日服药清单：把老人所有"进行中"的计划展开成一个个提醒时间点，并标注是否已打卡。
     */
    public TodayDoseSummary today(long elderId, LocalDate date) {
        long ownerId = CurrentUserHolder.requireUserId();
        Elder elder = requireOwnedElder(elderId, ownerId);
        LocalDate target = date == null ? LocalDate.now() : date;

        List<TodayDoseItem> items = new ArrayList<>();
        for (MedicationPlan plan : planMapper.findActiveByElderId(elderId, ownerId)) {
            if (!planValidOn(plan, target)) {
                continue;
            }
            for (String remindTime : remindTimesOf(plan)) {
                Optional<MedicationLog> log = logMapper
                        .findByPlanIdAndDateAndRemindTime(plan.getId(), target, remindTime, ownerId);
                items.add(new TodayDoseItem(
                        plan.getId(),
                        plan.getDrugName(),
                        plan.getDosage(),
                        plan.getFrequency(),
                        plan.getTakeRelation(),
                        remindTime,
                        log.isPresent(),
                        log.map(MedicationLog::getId).orElse(null),
                        log.map(MedicationLog::getStatus).orElse(null),
                        log.map(MedicationLog::getTakeTime).orElse(null)));
            }
        }
        items.sort(Comparator.comparing(TodayDoseItem::remindTime).thenComparing(TodayDoseItem::planId));

        int taken = (int) items.stream().filter(TodayDoseItem::taken).count();
        return new TodayDoseSummary(elder.getId(), elder.getName(), target, items.size(), taken,
                items.size() - taken, items);
    }

    /** 把 remindTimes 字符串（如 "08:00,20:00"）解析成时间点列表。 */
    public static List<String> remindTimesOf(MedicationPlan plan) {
        String raw = plan.getRemindTimes();
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
        if (plan.getStartDate() != null && date.isBefore(plan.getStartDate())) {
            return false;
        }
        return plan.getEndDate() == null || !date.isAfter(plan.getEndDate());
    }

    private Elder requireOwnedElder(long elderId, long ownerId) {
        return elderMapper.findById(elderId, ownerId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));
    }
}
