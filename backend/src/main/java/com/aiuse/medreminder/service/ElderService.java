package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.dto.ElderUpdateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.mapper.ElderMapper;
import com.aiuse.medreminder.mapper.MedicationPlanMapper;
import com.aiuse.medreminder.security.CurrentUserHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 老人档案业务层。
 * 每个账号只能增删改查自己的档案：所有操作都带当前登录用户 id。
 * 访问别人的档案时统一返回 404（而不是"无权限"），避免泄露数据是否存在。
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
        long ownerId = CurrentUserHolder.requireUserId();
        Elder elder = new Elder();
        elder.setOwnerId(ownerId);
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
        long ownerId = CurrentUserHolder.requireUserId();
        if (keyword == null || keyword.isBlank()) {
            return elderMapper.findAll(ownerId);
        }
        return elderMapper.search(ownerId, keyword.trim());
    }

    public Elder get(long id) {
        long ownerId = CurrentUserHolder.requireUserId();
        return elderMapper.findById(id, ownerId)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + id));
    }

    public Elder update(long id, ElderUpdateRequest request) {
        long ownerId = CurrentUserHolder.requireUserId();
        Elder elder = new Elder();
        elder.setId(id);
        elder.setOwnerId(ownerId);
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
        long ownerId = CurrentUserHolder.requireUserId();
        if (elderMapper.softDelete(id, ownerId) == 0) {
            throw BizException.notFound("老人档案不存在或已删除: id=" + id);
        }
        planMapper.softDeleteByElderId(id, ownerId);
    }
}

