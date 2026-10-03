package com.aiuse.medreminder.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 健康检查 Mapper。单条简单 SQL 用注解写更直观，复杂 SQL 统一放在 mapper/*.xml。
 */
@Mapper
public interface HealthMapper {

    @Select("SELECT 1")
    Integer ping();

    @Select("SELECT VERSION()")
    String mysqlVersion();

    @Select("SELECT DATABASE()")
    String schema();
}

