package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.CheckinRequest;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.mapper.ElderMapper;
import com.aiuse.medreminder.mapper.MedicationLogMapper;
import com.aiuse.medreminder.mapper.MedicationPlanMapper;
import com.aiuse.medreminder.security.CurrentUserHolder;
import com.aiuse.medreminder.vo.CheckinStats;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 服药打卡业务层：打卡、撤销、查询与按时率统计。
 * 只能操作自己老人的计划与打卡记录。
 */
@Service
public class CheckinService {

    private static final String DEFAULT_REMIND_TIME = "08:00";
    private static final String DEFAULT_STATUS = "TAKEN";
    private static final int MAX_STATS_DAYS = 92;

    private final MedicationLogMapper logMapper;
    private final MedicationPlanMapper planMapper;
    private final ElderMapper elderMapper;

    public CheckinService(MedicationLogMapper logMapper, MedicationPlanMapper planMapper,
                          ElderMapper elderMapper) {
        this.logMapper = logMapper;
        this.planMapper = planMapper;
        this.elderMapper = elderMapper;
    }

    public MedicationLog checkin(CheckinRequest request) {
        long ownerId = CurrentUserHolder.requireUserId();
        requireOwnedElder(request.elderId(), ownerId);
        MedicationPlan plan = planMapper.findById(request.planId(), ownerId)
                .orElseThrow(() -> BizException.notFound("用药计划不存在: id=" + request.planId()));
        if (!plan.getElderId().equals(request.elderId())) {
            throw BizException.badRequest("用药计划 " + plan.getId() + " 不属于老人 " + request.elderId());
        }
        String remindTime = request.remindTime() == null || request.remindTime().isBlank()
                ? DEFAULT_REMIND_TIME : request.remindTime();
        String status = request.status() == null || request.status().isBlank()
                ? DEFAULT_STATUS : request.status();
        if (logMapper.findByPlanIdAndDateAndRemindTime(plan.getId(), request.takeDate(), remindTime, ownerId)
                .isPresent()) {
            throw BizException.badRequest("该计划在 " + request.takeDate() + " " + remindTime
                    + " 已打卡，请勿重复提交");
        }

        MedicationLog log = new MedicationLog();
        log.setElderId(request.elderId());
        log.setPlanId(plan.getId());
        log.setTakeDate(request.takeDate());
        log.setRemindTime(remindTime);
        log.setTakeTime(LocalDateTime.now());
        log.setStatus(status);
        log.setNote(request.note());
        logMapper.insert(log);
        return logMapper.findById(log.getId(), ownerId).orElseThrow();
    }

    public List<MedicationLog> listByElder(long elderId, LocalDate date, LocalDate from, LocalDate to) {
        long ownerId = CurrentUserHolder.requireUserId();
        requireOwnedElder(elderId, ownerId);
        if (date != null) {
            return logMapper.findByElderIdAndDate(elderId, date, ownerId);
        }
        if (from != null || to != null) {
            LocalDate start = from != null ? from : to;
            LocalDate end = to != null ? to : from;
            if (start.isAfter(end)) {
                throw BizException.badRequest("开始日期不能晚于结束日期");
            }
            return logMapper.findByElderIdAndDateRange(elderId, start, end, ownerId);
        }
        return logMapper.findByElderId(elderId, ownerId);
    }

    /** 撤销误打卡（物理删除该条记录）。 */
    public void undo(long logId) {
        long ownerId = CurrentUserHolder.requireUserId();
        if (logMapper.deleteById(logId, ownerId) == 0) {
            throw BizException.notFound("打卡记录不存在: id=" + logId);
        }
    }

    /**
     * 按时率统计：应服次数来自"进行中计划 × 日期区间"的提醒时间点，
     * 已服次数来自该区间内状态为 TAKEN 的打卡记录。
     */
    public CheckinStats stats(long elderId, LocalDate from, LocalDate to) {
        long ownerId = CurrentUserHolder.requireUserId();
        requireOwnedElder(elderId, ownerId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(6);
        if (start.isAfter(end)) {
            throw BizException.badRequest("开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_STATS_DAYS) {
            throw BizException.badRequest("统计区间不能超过 " + MAX_STATS_DAYS + " 天");
        }

        List<MedicationPlan> plans = planMapper.findActiveByElderId(elderId, ownerId);
        int expected = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            for (MedicationPlan plan : plans) {
                if (MedicationPlanService.planValidOn(plan, day)) {
                    expected += MedicationPlanService.remindTimesOf(plan).size();
                }
            }
        }

        int taken = logMapper.countByStatusInRange(elderId, start, end, "TAKEN", ownerId);
        int missed = Math.max(0, expected - taken);
        double rate = expected == 0 ? 0d : Math.round(taken * 1000.0 / expected) / 1000.0;
        return new CheckinStats(elderId, start, end, expected, taken, missed, rate);
    }

    private void requireOwnedElder(long elderId, long ownerId) {
        elderMapper.findById(elderId, ownerId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));
    }
}

