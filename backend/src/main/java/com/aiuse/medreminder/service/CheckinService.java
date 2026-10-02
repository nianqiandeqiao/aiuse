package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.CheckinRequest;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.repository.ElderRepository;
import com.aiuse.medreminder.repository.MedicationLogRepository;
import com.aiuse.medreminder.repository.MedicationPlanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CheckinService {

    private static final String DEFAULT_REMIND_TIME = "08:00";
    private static final String DEFAULT_STATUS = "TAKEN";

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
        elderRepository.findById(request.elderId())
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + request.elderId()));
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

    public List<MedicationLog> listByElder(long elderId, LocalDate takeDate) {
        elderRepository.findById(elderId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + elderId));
        return takeDate == null
                ? logRepository.findByElderId(elderId)
                : logRepository.findByElderIdAndDate(elderId, takeDate);
    }
}

