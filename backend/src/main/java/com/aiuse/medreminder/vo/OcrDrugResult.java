package com.aiuse.medreminder.vo;

import com.aiuse.medreminder.service.DrugDictionary.DrugHit;

import java.util.List;

/**
 * 拍照识药的结果：原始文字 + 分行 + 命中的药品。
 */
public record OcrDrugResult(
        String rawText,
        List<String> lines,
        List<DrugHit> drugs,
        int lineCount,
        int drugCount) {

    public static OcrDrugResult of(List<String> lines, List<DrugHit> drugs) {
        String raw = String.join("\n", lines);
        return new OcrDrugResult(raw, lines, drugs, lines.size(), drugs.size());
    }
}

