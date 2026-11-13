package com.smartslot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartslot.entity.MatchChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拼场搭子实时聊天消息 Mapper 接口
 */
@Mapper
public interface MatchChatMessageMapper extends BaseMapper<MatchChatMessage> {
}
