package com.aiuse.medreminder.mapper;

import com.aiuse.medreminder.entity.MedicationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 服药打卡 Mapper（持久层），SQL 写在 resources/mapper/MedicationLogMapper.xml。
 */
@Mapper
public interface MedicationLogMapper {

    int insert(MedicationLog log);

    Optional<MedicationLog> findById(@Param("id") long id);

    List<MedicationLog> findByElderId(@Param("elderId") long elderId);

    List<MedicationLog> findByElderIdAndDate(@Param("elderId") long elderId,
                                             @Param("takeDate") LocalDate takeDate);

    List<MedicationLog> findByElderIdAndDateRange(@Param("elderId") long elderId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    Optional<MedicationLog> findByPlanIdAndDateAndRemindTime(@Param("planId") long planId,
                                                             @Param("takeDate") LocalDate takeDate,
                                                             @Param("remindTime") String remindTime);

    int countByStatusInRange(@Param("elderId") long elderId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to,
                             @Param("status") String status);

    int deleteById(@Param("id") long id);
}

