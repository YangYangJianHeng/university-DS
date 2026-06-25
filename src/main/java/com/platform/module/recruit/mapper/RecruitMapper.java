package com.platform.module.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.module.recruit.entity.Recruit;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface RecruitMapper extends BaseMapper<Recruit> {

    List<Recruit> selectByFilters(@Param("projectType") String projectType,
                                  @Param("status") String status);

    @Update("UPDATE recruit SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);
}
