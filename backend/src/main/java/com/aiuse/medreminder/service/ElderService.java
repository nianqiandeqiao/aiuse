package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.dto.ElderUpdateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.repository.ElderRepository;
import com.aiuse.medreminder.repository.MedicationPlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElderService {

    private final ElderRepository elderRepository;
    private final MedicationPlanRepository planRepository;

    public ElderService(ElderRepository elderRepository, MedicationPlanRepository planRepository) {
        this.elderRepository = elderRepository;
        this.planRepository = planRepository;
    }

    public Elder create(ElderCreateRequest request) {
        return elderRepository.insert(request);
    }

    public List<Elder> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return elderRepository.findAll();
        }
        return elderRepository.search(keyword.trim());
    }

    public Elder get(long id) {
        return elderRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + id));
    }

    public Elder update(long id, ElderUpdateRequest request) {
        int rows = elderRepository.update(id, request);
        if (rows == 0) {
            throw BizException.notFound("老人档案不存在: id=" + id);
        }
        return get(id);
    }

    public void delete(long id) {
        int rows = elderRepository.softDelete(id);
        if (rows == 0) {
            throw BizException.notFound("老人档案不存在或已删除: id=" + id);
        }
        planRepository.softDeleteByElderId(id);
    }
}
