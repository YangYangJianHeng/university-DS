package com.platform.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.module.ai.entity.AIChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AIChatMessageMapper extends BaseMapper<AIChatMessage> {
}
