package com.aiuse.medreminder.mapper;

import com.aiuse.medreminder.entity.InteractionRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 相互作用规则 Mapper，SQL 写在 resources/mapper/InteractionRuleMapper.xml。
 */
@Mapper
public interface InteractionRuleMapper {

    List<InteractionRule> findAllEnabled();

    List<InteractionRule> findAll();

    Optional<InteractionRule> findById(@Param("id") long id);
}

