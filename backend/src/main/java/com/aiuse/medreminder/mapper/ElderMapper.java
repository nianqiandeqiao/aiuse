package com.aiuse.medreminder.mapper;

import com.aiuse.medreminder.entity.Elder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 老人档案 Mapper（持久层），SQL 写在 resources/mapper/ElderMapper.xml。
 */
@Mapper
public interface ElderMapper {

    int insert(Elder elder);

    List<Elder> findAll();

    List<Elder> search(@Param("keyword") String keyword);

    Optional<Elder> findById(@Param("id") long id);

    int update(Elder elder);

    int softDelete(@Param("id") long id);
}

