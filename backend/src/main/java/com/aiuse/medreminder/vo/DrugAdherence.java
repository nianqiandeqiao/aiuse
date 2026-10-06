package com.aiuse.medreminder.vo;

/** 单个药品的依从性统计。 */
public record DrugAdherence(
        Long planId,
        String drugName,
        String dosage,
        int expected,
        int taken,
        int missed,
        double rate) {
}

