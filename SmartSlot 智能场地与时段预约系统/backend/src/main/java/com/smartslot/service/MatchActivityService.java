package com.smartslot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartslot.dto.MatchCreateDto;
import com.smartslot.dto.MatchQueryDto;
import com.smartslot.entity.MatchActivity;

import java.util.List;

/**
 * 拼场约球与搭子大厅核心服务接口
 */
public interface MatchActivityService extends IService<MatchActivity> {

    /**
     * 发起拼场招募
     */
    MatchActivity createMatch(MatchCreateDto dto, Long userId);

    /**
     * 拼场搭子大厅分页检索
     */
    Page<MatchActivity> pageMatches(MatchQueryDto queryDto, Long currentUserId);

    /**
     * 获取拼场详情 (含成员列表)
     */
    MatchActivity getMatchDetail(Long id, Long currentUserId);

    /**
     * 球友一键加入拼场 (AA分摊扣款，满员自动成团并分发专属核销码)
     */
    MatchActivity joinMatch(Long activityId, Long userId);

    /**
     * 取消/解散拼场活动 (原路退回各成员钱包并释放时段)
     */
    void cancelMatch(Long activityId, Long userId);

    /**
     * 查询我的拼场活动 (发起 + 参与)
     */
    List<MatchActivity> listMyMatches(Long userId);
}
