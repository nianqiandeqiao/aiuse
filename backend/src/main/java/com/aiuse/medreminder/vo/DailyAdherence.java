package com.aiuse.medreminder.vo;

import java.time.LocalDate;

/** 某一天的应服/已服/漏服次数，用于图表。 */
public record DailyAdherence(LocalDate date, int expected, int taken, int missed) {
}

