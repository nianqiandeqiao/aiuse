package com.aiuse.medreminder.mapper;

import com.aiuse.medreminder.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface AppUserMapper {

    int insert(AppUser user);

    Optional<AppUser> findByUsername(@Param("username") String username);

    Optional<AppUser> findById(@Param("id") long id);

    int countUsers();

    /** 把还没有归属的历史数据（演示数据）划给第一个注册的账号。 */
    int claimOrphanElders(@Param("ownerId") long ownerId);
}
