package com.platform.module.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.module.competition.entity.Competition;
import org.apache.ibatis.annotations.Update;

public interface CompetitionMapper extends BaseMapper<Competition> {

    @Update("UPDATE competition SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);
}
