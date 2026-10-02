package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.repository.ElderRepository;
import com.aiuse.medreminder.repository.MedicationPlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicationPlanService {

    private final MedicationPlanRepository planRepository;
    private final ElderRepository elderRepository;

    public MedicationPlanService(MedicationPlanRepository planRepository, ElderRepository elderRepository) {
        this.planRepository = planRepository;
        this.elderRepository = elderRepository;
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
}

