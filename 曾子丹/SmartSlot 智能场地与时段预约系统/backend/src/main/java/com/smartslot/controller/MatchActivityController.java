package com.smartslot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartslot.annotation.Idempotent;
import com.smartslot.annotation.LogRecord;
import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.dto.MatchCreateDto;
import com.smartslot.dto.MatchQueryDto;
import com.smartslot.entity.MatchActivity;
import com.smartslot.service.MatchActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "拼场约球与搭子大厅接口")
@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchActivityController {

    private final MatchActivityService matchActivityService;

    @Operation(summary = "发起拼场招募")
    @LogRecord(module = "拼场约球", operation = "发起拼场")
    @PostMapping("/create")
    public Result<MatchActivity> createMatch(@Valid @RequestBody MatchCreateDto dto) {
        Long userId = UserContext.getUserId();
        MatchActivity activity = matchActivityService.createMatch(dto, userId);
        return Result.success("拼场招募发起成功，时段已为您锁定！", activity);
    }

    @Operation(summary = "拼场大厅分页列表")
    @GetMapping("/page")
    public Result<Page<MatchActivity>> pageMatches(MatchQueryDto queryDto) {
        Long currentUserId = UserContext.getUserId();
        Page<MatchActivity> page = matchActivityService.pageMatches(queryDto, currentUserId);
        return Result.success(page);
    }

    @Operation(summary = "获取拼场招募详情")
    @GetMapping("/{id}")
    public Result<MatchActivity> getDetail(@PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        MatchActivity detail = matchActivityService.getMatchDetail(id, currentUserId);
        return Result.success(detail);
    }

    @Operation(summary = "球友一键上车加入拼场")
    @LogRecord(module = "拼场约球", operation = "加入拼场")
    @PostMapping("/{id}/join")
    public Result<MatchActivity> joinMatch(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        MatchActivity act = matchActivityService.joinMatch(id, userId);
        return Result.success("成功加入拼场！", act);
    }

    @Operation(summary = "发起人取消/解散拼场")
    @LogRecord(module = "拼场约球", operation = "解散拼场")
    @PostMapping("/{id}/cancel")
    public Result<String> cancelMatch(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        matchActivityService.cancelMatch(id, userId);
        return Result.success("拼场已解散，款项已原路退回各成员钱包");
    }

    @Operation(summary = "查询我的拼场记录 (我发起的与我加入的)")
    @GetMapping("/my")
    public Result<List<MatchActivity>> listMy() {
        Long userId = UserContext.getUserId();
        List<MatchActivity> list = matchActivityService.listMyMatches(userId);
        return Result.success(list);
    }
}
