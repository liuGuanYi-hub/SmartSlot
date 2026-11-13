package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.common.BusinessException;
import com.smartslot.dto.MatchChatSendDto;
import com.smartslot.entity.MatchActivity;
import com.smartslot.entity.MatchChatMessage;
import com.smartslot.entity.MatchParticipant;
import com.smartslot.entity.SysUser;
import com.smartslot.mapper.MatchActivityMapper;
import com.smartslot.mapper.MatchChatMessageMapper;
import com.smartslot.mapper.MatchParticipantMapper;
import com.smartslot.mapper.SysUserMapper;
import com.smartslot.service.MatchChatService;
import com.smartslot.websocket.MatchChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼场搭子实时微聊服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchChatServiceImpl extends ServiceImpl<MatchChatMessageMapper, MatchChatMessage> implements MatchChatService {

    private final MatchActivityMapper matchActivityMapper;
    private final MatchParticipantMapper matchParticipantMapper;
    private final SysUserMapper sysUserMapper;
    private final MatchChatWebSocketHandler matchChatWebSocketHandler;

    @Override
    public List<MatchChatMessage> listHistory(Long activityId, Long userId) {
        MatchActivity activity = matchActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "拼场招募活动不存在");
        }

        // 查询该房间最近 100 条聊天记录 (按时间正序排列)
        return list(new LambdaQueryWrapper<MatchChatMessage>()
                .eq(MatchChatMessage::getActivityId, activityId)
                .orderByAsc(MatchChatMessage::getCreateTime)
                .last("LIMIT 100"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchChatMessage sendAndBroadcast(MatchChatSendDto dto, Long userId) {
        Long activityId = dto.getActivityId();
        MatchActivity activity = matchActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "拼场招募活动不存在");
        }
        if (activity.getStatus() == 3) {
            throw new BusinessException(400, "该拼场已解散退款，微聊室已关闭发言");
        }

        SysUser sender = sysUserMapper.selectById(userId);
        if (sender == null) {
            throw new BusinessException(401, "发送用户不存在或未登录");
        }

        boolean isCreator = activity.getCreatorId().equals(userId);
        boolean isParticipant = matchParticipantMapper.selectCount(new LambdaQueryWrapper<MatchParticipant>()
                .eq(MatchParticipant::getActivityId, activityId)
                .eq(MatchParticipant::getUserId, userId)
                .eq(MatchParticipant::getPayStatus, 1)) > 0;

        // 若已成团 (status == 1)，仅限已成团球友和发起人在专属微室发言
        if (activity.getStatus() == 1 && !isCreator && !isParticipant) {
            throw new BusinessException(403, "该活动已满员锁定成团，仅限已上车球友内部沟通");
        }

        MatchChatMessage message = MatchChatMessage.builder()
                .activityId(activityId)
                .userId(userId)
                .username(sender.getUsername())
                .nickname(sender.getNickname() != null ? sender.getNickname() : sender.getUsername())
                .avatar(sender.getAvatar() != null ? sender.getAvatar() : "")
                .isCreator(isCreator ? 1 : 0)
                .content(dto.getContent().trim())
                .msgType(dto.getMsgType() != null ? dto.getMsgType() : "TEXT")
                .createTime(LocalDateTime.now())
                .build();

        // 1. 持久化落库
        save(message);

        // 2. 通过 WebSocket 毫秒级广播至当前拼场活动房间的所有在线客户端
        matchChatWebSocketHandler.broadcastToRoom(activityId, message);

        log.info("[搭子微聊] 发送消息成功: activityId={}, sender={}, msgType={}",
                activityId, sender.getUsername(), message.getMsgType());
        return message;
    }
}
