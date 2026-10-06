package com.aiuse.medreminder.vo;

/**
 * 药品库存/有效期提醒：临期、已过期、快吃完、已用完。
 */
public record StockAlert(
        String type,
        Long planId,
        String drugName,
        String title,
        String detail,
        Integer days) {

    public static final String TYPE_EXPIRED = "EXPIRED";
    public static final String TYPE_EXPIRING = "EXPIRING";
    public static final String TYPE_LOW_STOCK = "LOW_STOCK";
    public static final String TYPE_OUT_OF_STOCK = "OUT_OF_STOCK";
}

