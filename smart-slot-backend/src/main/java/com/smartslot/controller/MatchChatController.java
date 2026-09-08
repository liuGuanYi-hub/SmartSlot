package com.smartslot.controller;

import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.dto.MatchChatSendDto;
import com.smartslot.entity.MatchChatMessage;
import com.smartslot.service.MatchChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 拼场搭子实时微聊接口
 */
@Slf4j
@Tag(name = "拼场搭子微聊室接口")
@RestController
@RequestMapping("/api/match/{activityId}/chat")
@RequiredArgsConstructor
public class MatchChatController {

    private final MatchChatService matchChatService;

    @Operation(summary = "获取拼场活动历史聊天消息")
    @GetMapping("/history")
    public Result<List<MatchChatMessage>> getChatHistory(@PathVariable Long activityId) {
        Long userId = UserContext.getUserId();
        List<MatchChatMessage> history = matchChatService.listHistory(activityId, userId);
        return Result.success(history);
    }

    @Operation(summary = "发送聊天消息 (同时落库并 WebSocket 全房间广播)")
    @PostMapping("/send")
    public Result<MatchChatMessage> sendMessage(
            @PathVariable Long activityId,
            @Valid @RequestBody MatchChatSendDto dto) {
        Long userId = UserContext.getUserId();
        dto.setActivityId(activityId);
        MatchChatMessage savedMessage = matchChatService.sendAndBroadcast(dto, userId);
        return Result.success("发送成功", savedMessage);
    }
}
