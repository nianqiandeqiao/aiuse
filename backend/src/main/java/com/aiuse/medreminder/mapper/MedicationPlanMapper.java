package com.aiuse.medreminder.mapper;

import com.aiuse.medreminder.entity.MedicationPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用药计划 Mapper。计划本身没有 owner_id，归属通过 elder.owner_id 判断，
 * 所有查询都 join elder 并带 ownerId 条件。
 */
@Mapper
public interface MedicationPlanMapper {

    int insert(MedicationPlan plan);

    Optional<MedicationPlan> findById(@Param("id") long id, @Param("ownerId") long ownerId);

    List<MedicationPlan> findByElderId(@Param("elderId") long elderId, @Param("ownerId") long ownerId);

    List<MedicationPlan> findActiveByElderId(@Param("elderId") long elderId,
                                             @Param("ownerId") long ownerId);

    int update(@Param("plan") MedicationPlan plan, @Param("ownerId") long ownerId);

    int updateStatus(@Param("id") long id, @Param("status") int status,
                     @Param("ownerId") long ownerId);

    int softDelete(@Param("id") long id, @Param("ownerId") long ownerId);

    int softDeleteByElderId(@Param("elderId") long elderId, @Param("ownerId") long ownerId);

    /** 历史用药：已停用或已删除的计划（数据保留，方便以后查找）。 */
    List<com.aiuse.medreminder.vo.MedicineHistoryItem> findHistoryByElderId(
            @Param("elderId") long elderId, @Param("ownerId") long ownerId);
}
