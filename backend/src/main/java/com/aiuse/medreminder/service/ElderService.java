package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.dto.ElderUpdateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.mapper.ElderMapper;
import com.aiuse.medreminder.mapper.MedicationPlanMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 老人档案业务层：只做业务判断，SQL 全部交给 ElderMapper。
 */
@Service
public class ElderService {

    private final ElderMapper elderMapper;
    private final MedicationPlanMapper planMapper;

    public ElderService(ElderMapper elderMapper, MedicationPlanMapper planMapper) {
        this.elderMapper = elderMapper;
        this.planMapper = planMapper;
    }

    public Elder create(ElderCreateRequest request) {
        Elder elder = new Elder();
        elder.setName(request.name());
        elder.setAge(request.age());
        elder.setGender(request.gender());
        elder.setPhone(request.phone());
        elder.setChronicDisease(request.chronicDisease());
        elder.setNote(request.note());
        elderMapper.insert(elder);
        return get(elder.getId());
    }

    public List<Elder> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return elderMapper.findAll();
        }
        return elderMapper.search(keyword.trim());
    }

    public Elder get(long id) {
        return elderMapper.findById(id)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + id));
    }

    public Elder update(long id, ElderUpdateRequest request) {
        Elder elder = new Elder();
        elder.setId(id);
        elder.setName(request.name());
        elder.setAge(request.age());
        elder.setGender(request.gender());
        elder.setPhone(request.phone());
        elder.setChronicDisease(request.chronicDisease());
        elder.setNote(request.note());
        if (elderMapper.update(elder) == 0) {
            throw BizException.notFound("老人档案不存在: id=" + id);
        }
        return get(id);
    }

    public void delete(long id) {
        if (elderMapper.softDelete(id) == 0) {
            throw BizException.notFound("老人档案不存在或已删除: id=" + id);
        }
        planMapper.softDeleteByElderId(id);
    }
}

