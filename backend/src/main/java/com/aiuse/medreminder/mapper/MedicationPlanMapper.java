package com.aiuse.medreminder.mapper;

import com.aiuse.medreminder.entity.MedicationPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用药计划 Mapper（持久层），SQL 写在 resources/mapper/MedicationPlanMapper.xml。
 */
@Mapper
public interface MedicationPlanMapper {

    int insert(MedicationPlan plan);

    Optional<MedicationPlan> findById(@Param("id") long id);

    List<MedicationPlan> findByElderId(@Param("elderId") long elderId);

    List<MedicationPlan> findActiveByElderId(@Param("elderId") long elderId);

    int update(MedicationPlan plan);

    int updateStatus(@Param("id") long id, @Param("status") int status);

    int softDelete(@Param("id") long id);

    int softDeleteByElderId(@Param("elderId") long elderId);
}

