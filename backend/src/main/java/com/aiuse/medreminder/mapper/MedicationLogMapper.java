package com.aiuse.medreminder.mapper;

import com.aiuse.medreminder.entity.MedicationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 服药打卡 Mapper。归属通过 elder.owner_id 判断。
 */
@Mapper
public interface MedicationLogMapper {

    int insert(MedicationLog log);

    Optional<MedicationLog> findById(@Param("id") long id, @Param("ownerId") long ownerId);

    List<MedicationLog> findByElderId(@Param("elderId") long elderId, @Param("ownerId") long ownerId);

    List<MedicationLog> findByElderIdAndDate(@Param("elderId") long elderId,
                                             @Param("takeDate") LocalDate takeDate,
                                             @Param("ownerId") long ownerId);

    List<MedicationLog> findByElderIdAndDateRange(@Param("elderId") long elderId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to,
                                                  @Param("ownerId") long ownerId);

    Optional<MedicationLog> findByPlanIdAndDateAndRemindTime(@Param("planId") long planId,
                                                             @Param("takeDate") LocalDate takeDate,
                                                             @Param("remindTime") String remindTime,
                                                             @Param("ownerId") long ownerId);

    int countByStatusInRange(@Param("elderId") long elderId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to,
                             @Param("status") String status,
                             @Param("ownerId") long ownerId);

    int deleteById(@Param("id") long id, @Param("ownerId") long ownerId);
}

