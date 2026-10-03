package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.CheckinRequest;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.repository.ElderRepository;
import com.aiuse.medreminder.repository.MedicationLogRepository;
import com.aiuse.medreminder.repository.MedicationPlanRepository;
import com.aiuse.medreminder.vo.CheckinStats;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 服药打卡：打卡、撤销、查询与按时率统计。
 */
@Service
public class CheckinService {

    private static final String DEFAULT_REMIND_TIME = "08:00";
    private static final String DEFAULT_STATUS = "TAKEN";
    private static final int MAX_STATS_DAYS = 92;

    private final MedicationLogRepository logRepository;
    private final MedicationPlanRepository planRepository;
    private final ElderRepository elderRepository;

    public CheckinService(MedicationLogRepository logRepository, MedicationPlanRepository planRepository,
                          ElderRepository elderRepository) {
        this.logRepository = logRepository;
        this.planRepository = planRepository;
        this.elderRepository = elderRepository;
    }

    public MedicationLog checkin(CheckinRequest request) {
        requireElder(request.elderId());
        MedicationPlan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> BizException.notFound("用药计划不存在: id=" + request.planId()));
        if (!plan.elderId().equals(request.elderId())) {
            throw BizException.badRequest("用药计划 " + plan.id() + " 不属于老人 " + request.elderId());
        }
        String remindTime = request.remindTime() == null || request.remindTime().isBlank()
                ? DEFAULT_REMIND_TIME : request.remindTime();
        String status = request.status() == null || request.status().isBlank()
                ? DEFAULT_STATUS : request.status();
        if (logRepository.findByPlanIdAndDateAndRemindTime(plan.id(), request.takeDate(), remindTime)
                .isPresent()) {
            throw BizException.badRequest("该计划在 " + request.takeDate() + " " + remindTime
                    + " 已打卡，请勿重复提交");
        }
        return logRepository.insert(request.elderId(), plan.id(), request.takeDate(), remindTime,
                status, request.note());
    }

    /**
     * 查询打卡记录：date 优先；否则用 from/to 区间；都不传则返回全部。
     */
    public List<MedicationLog> listByElder(long elderId, LocalDate date, LocalDate from, LocalDate to) {
        requireElder(elderId);
        if (date != null) {
            return logRepository.findByElderIdAndDate(elderId, date);
        }
        if (from != null || to != null) {
            LocalDate start = from != null ? from : to;
            LocalDate end = to != null ? to : from;
            if (start.isAfter(end)) {
                throw BizException.badRequest("开始日期不能晚于结束日期");
            }
            return logRepository.findByElderIdAndDateRange(elderId, start, end);
        }
        return logRepository.findByElderId(elderId);
    }

    /** 撤销误打卡（物理删除该条记录）。 */
    public void undo(long logId) {
        int rows = logRepository.deleteById(logId);
        if (rows == 0) {
            throw BizException.notFound("打卡记录不存在: id=" + logId);
        }
    }

    /**
     * 按时率统计：应服次数来自"进行中计划 × 日期区间"的提醒时间点，
     * 已服次数来自该区间内状态为 TAKEN 的打卡记录。
     */
    public CheckinStats stats(long elderId, LocalDate from, LocalDate to) {
        requireElder(elderId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(6);
        if (start.isAfter(end)) {
            throw BizException.badRequest("开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_STATS_DAYS) {
            throw BizException.badRequest("统计区间不能超过 " + MAX_STATS_DAYS + " 天");
        }

        List<MedicationPlan> plans = planRepository.findActiveByElderId(elderId);
        int expected = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            for (MedicationPlan plan : plans) {
                if (MedicationPlanService.planValidOn(plan, day)) {
                    expected += MedicationPlanService.remindTimesOf(plan).size();
                }
            }
        }

        int taken = logRepository.countByStatusInRange(elderId, start, end, "TAKEN");
        int missed = Math.max(0, expected - taken);
        double rate = expected == 0 ? 0d : Math.round(taken * 1000.0 / expected) / 1000.0;
        return new CheckinStats(elderId, start, end, expected, taken, missed, rate);
    }

    private void requireElder(long elderId) {
        elderRepository.findById(elderId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));
    }
}

