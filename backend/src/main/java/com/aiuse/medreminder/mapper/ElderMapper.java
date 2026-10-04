package com.aiuse.medreminder.mapper;

import com.aiuse.medreminder.entity.Elder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 老人档案 Mapper（持久层）。
 * 所有查询都带 ownerId 条件，保证一个账号只能看到自己的档案。
 */
@Mapper
public interface ElderMapper {

    int insert(Elder elder);

    List<Elder> findAll(@Param("ownerId") long ownerId);

    List<Elder> search(@Param("ownerId") long ownerId, @Param("keyword") String keyword);

    Optional<Elder> findById(@Param("id") long id, @Param("ownerId") long ownerId);

    int update(Elder elder);

    int softDelete(@Param("id") long id, @Param("ownerId") long ownerId);

    /** 管理员/迁移用：不带归属条件地检查档案是否存在。 */
    Optional<Elder> findByIdIgnoreOwner(@Param("id") long id);
}

