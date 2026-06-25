package com.platform.module.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.module.recruit.entity.Application;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ApplicationMapper extends BaseMapper<Application> {

    @Select("SELECT COUNT(*) > 0 FROM application WHERE recruit_id = #{recruitId} AND applicant_id = #{userId}")
    boolean existsByRecruitAndUser(@Param("recruitId") Long recruitId,
                                   @Param("userId") Long userId);
}
