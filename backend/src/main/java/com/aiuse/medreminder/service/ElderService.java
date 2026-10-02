package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.repository.ElderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElderService {

    private final ElderRepository elderRepository;

    public ElderService(ElderRepository elderRepository) {
        this.elderRepository = elderRepository;
    }

    public Elder create(ElderCreateRequest request) {
        return elderRepository.insert(request);
    }

    public List<Elder> list() {
        return elderRepository.findAll();
    }

    public Elder get(long id) {
        return elderRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("老人档案不存在: id=" + id));
    }

    public void delete(long id) {
        int rows = elderRepository.softDelete(id);
        if (rows == 0) {
            throw BizException.notFound("老人档案不存在或已删除: id=" + id);
        }
    }
}

