package com.aiuse.medreminder.vo;

import com.aiuse.medreminder.service.DrugDictionary.DrugHit;

import java.time.LocalDate;
import java.util.List;

/**
 * 药盒说明书分析结果：识别到的药品、有效期判断、与在服药的冲突、用药建议。
 */
public record MedicineAnalysis(
        List<DrugHit> drugs,
        String expireText,
        LocalDate expireDate,
        Boolean expired,
        List<InteractionConflict> conflicts,
        List<String> advices,
        String summary,
        String source) {
}

