package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.common.BusinessException;
import com.smartslot.dto.MatchCreateDto;
import com.smartslot.dto.MatchQueryDto;
import com.smartslot.dto.SlotEventDto;
import com.smartslot.entity.*;
import com.smartslot.mapper.*;
import com.smartslot.service.MatchActivityService;
import com.smartslot.service.WebSocketPushService;
import com.smartslot.util.LuaLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchActivityServiceImpl extends ServiceImpl<MatchActivityMapper, MatchActivity> implements MatchActivityService {

    private final MatchParticipantMapper participantMapper;
    private final VenueMapper venueMapper;
    private final VenueCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final LuaLockManager luaLockManager;
    private final WebSocketPushService webSocketPushService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchActivity createMatch(MatchCreateDto dto, Long userId) {
        Venue venue = venueMapper.selectById(dto.getVenueId());
        if (venue == null || venue.getStatus() == 0) {
            throw new BusinessException("该场地不存在或正在维护中");
        }

        if (dto.getBookDate().isBefore(LocalDate.now())) {
            throw new BusinessException("不能发起过去日期的拼场");
        }

        String lockKey = "SLOT:LOCK:" + dto.getVenueId() + ":" + dto.getBookDate() + ":" + dto.getTimeSlot();
        boolean lockSuccess = luaLockManager.tryLockAtomic(lockKey, "MATCH:" + userId, 3600); // 拼场招募预占
        if (!lockSuccess) {
            throw new BusinessException("该时段已被其他球友锁定或已发起拼场，请选择其他时段");
        }

        // 双重校验数据库是否已有有效订单
        BookingOrder existingOrder = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getVenueId, dto.getVenueId())
                .eq(BookingOrder::getBookDate, dto.getBookDate())
                .eq(BookingOrder::getTimeSlot, dto.getTimeSlot())
                .in(BookingOrder::getOrderStatus, 0, 1, 2)
                .last("LIMIT 1"));
        if (existingOrder != null) {
            throw new BusinessException("该时段已有预约订单，无法发起拼场");
        }

        SysUser creator = userMapper.selectById(userId);
        if (creator == null) {
            throw new BusinessException("用户不存在");
        }

        VenueCategory category = categoryMapper.selectById(venue.getCategoryId());
        String categoryName = category != null ? category.getName() : "综合运动";

        BigDecimal totalAmount = venue.getPricePerHour();
        BigDecimal costPerPerson = totalAmount.divide(BigDecimal.valueOf(dto.getTargetMembers()), 2, RoundingMode.HALF_UP);

        // 校验发起人余额
        if (creator.getBalance().compareTo(costPerPerson) < 0) {
            throw new BusinessException("账户余额不足以支付首付 AA 份额 (需 ￥" + costPerPerson + "，当前余额 ￥" + creator.getBalance() + ")");
        }

        // 扣除发起人 AA 份额
        creator.setBalance(creator.getBalance().subtract(costPerPerson));
        userMapper.updateById(creator);

        LocalDateTime now = LocalDateTime.now();
        String activityNo = "ACT" + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));

        MatchActivity activity = MatchActivity.builder()
                .activityNo(activityNo)
                .creatorId(userId)
                .venueId(dto.getVenueId())
                .venueName(venue.getName())
                .categoryName(categoryName)
                .bookDate(dto.getBookDate())
                .timeSlot(dto.getTimeSlot())
                .title(dto.getTitle())
                .sportTag(dto.getSportTag())
                .targetMembers(dto.getTargetMembers())
                .currentMembers(1)
                .totalAmount(totalAmount)
                .costPerPerson(costPerPerson)
                .description(dto.getDescription())
                .status(0) // 0-招募中
                .expireTime(now.plusDays(1))
                .createTime(now)
                .updateTime(now)
                .build();

        save(activity);

        // 创建发起人成员记录
        MatchParticipant p = MatchParticipant.builder()
                .activityId(activity.getId())
                .userId(userId)
                .username(creator.getUsername())
                .nickname(creator.getNickname() != null ? creator.getNickname() : creator.getUsername())
                .avatar(creator.getAvatar())
                .payAmount(costPerPerson)
                .payStatus(1) // 已支付
                .isCreator(1)
                .joinTime(now)
                .build();
        participantMapper.insert(p);

        // 广播时段锁定与拼场动态
        webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                .eventType("LOCK")
                .venueId(venue.getId())
                .venueName(venue.getName())
                .bookDate(dto.getBookDate())
                .timeSlot(dto.getTimeSlot())
                .status(1)
                .userId(userId)
                .message("【拼场发起】" + creator.getNickname() + " 发起了 " + venue.getName() + " 的约球招募！")
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("成功发起拼场招募: activityNo={}, creator={}, title={}", activityNo, creator.getUsername(), dto.getTitle());
        return activity;
    }

    @Override
    public Page<MatchActivity> pageMatches(MatchQueryDto queryDto, Long currentUserId) {
        Page<MatchActivity> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
        LambdaQueryWrapper<MatchActivity> wrapper = new LambdaQueryWrapper<MatchActivity>()
                .eq(StringUtils.hasText(queryDto.getCategoryName()), MatchActivity::getCategoryName, queryDto.getCategoryName())
                .eq(queryDto.getStatus() != null, MatchActivity::getStatus, queryDto.getStatus())
                .eq(queryDto.getBookDate() != null, MatchActivity::getBookDate, queryDto.getBookDate())
                .and(StringUtils.hasText(queryDto.getKeyword()), w -> w
                        .like(MatchActivity::getTitle, queryDto.getKeyword())
                        .or().like(MatchActivity::getVenueName, queryDto.getKeyword())
                        .or().like(MatchActivity::getSportTag, queryDto.getKeyword()))
                .orderByAsc(MatchActivity::getStatus) // 招募中排前面
                .orderByDesc(MatchActivity::getCreateTime);

        Page<MatchActivity> result = page(page, wrapper);
        if (result.getRecords().isEmpty()) {
            return result;
        }

        // 批量装配发起人与当前用户参与状态
        Set<Long> creatorIds = result.getRecords().stream().map(MatchActivity::getCreatorId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        Set<Long> activityIds = result.getRecords().stream().map(MatchActivity::getId).collect(Collectors.toSet());
        List<MatchParticipant> allParticipants = participantMapper.selectList(new LambdaQueryWrapper<MatchParticipant>()
                .in(MatchParticipant::getActivityId, activityIds)
                .eq(MatchParticipant::getPayStatus, 1));
        Map<Long, List<MatchParticipant>> participantMap = allParticipants.stream()
                .collect(Collectors.groupingBy(MatchParticipant::getActivityId));

        for (MatchActivity act : result.getRecords()) {
            SysUser u = userMap.get(act.getCreatorId());
            if (u != null) {
                act.setCreatorName(u.getNickname() != null ? u.getNickname() : u.getUsername());
                act.setCreatorAvatar(u.getAvatar());
                act.setCreatorCreditScore(u.getCreditScore() != null ? u.getCreditScore() : 100);
            }

            List<MatchParticipant> pList = participantMap.getOrDefault(act.getId(), Collections.emptyList());
            act.setParticipants(pList);
            if (currentUserId != null) {
                boolean joined = pList.stream().anyMatch(p -> p.getUserId().equals(currentUserId));
                act.setIsJoined(joined);
                if (joined) {
                    pList.stream().filter(p -> p.getUserId().equals(currentUserId)).findFirst()
                            .ifPresent(p -> act.setMyVerifyCode(p.getVerifyCode()));
                }
            }
        }

        return result;
    }

    @Override
    public MatchActivity getMatchDetail(Long id, Long currentUserId) {
        MatchActivity act = getById(id);
        if (act == null) {
            throw new BusinessException("拼场活动不存在");
        }

        SysUser creator = userMapper.selectById(act.getCreatorId());
        if (creator != null) {
            act.setCreatorName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
            act.setCreatorAvatar(creator.getAvatar());
            act.setCreatorCreditScore(creator.getCreditScore() != null ? creator.getCreditScore() : 100);
        }

        List<MatchParticipant> pList = participantMapper.selectList(new LambdaQueryWrapper<MatchParticipant>()
                .eq(MatchParticipant::getActivityId, id)
                .eq(MatchParticipant::getPayStatus, 1)
                .orderByAsc(MatchParticipant::getJoinTime));
        act.setParticipants(pList);

        if (currentUserId != null) {
            boolean joined = pList.stream().anyMatch(p -> p.getUserId().equals(currentUserId));
            act.setIsJoined(joined);
            if (joined) {
                pList.stream().filter(p -> p.getUserId().equals(currentUserId)).findFirst()
                        .ifPresent(p -> act.setMyVerifyCode(p.getVerifyCode()));
            }
        }
        return act;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchActivity joinMatch(Long activityId, Long userId) {
        MatchActivity act = getById(activityId);
        if (act == null) {
            throw new BusinessException("拼场活动不存在");
        }

        if (act.getStatus() != 0) {
            throw new BusinessException("当前拼场活动已满员或已截止，无法加入");
        }

        if (act.getCurrentMembers() >= act.getTargetMembers()) {
            throw new BusinessException("该拼场已经满员啦");
        }

        // 校验是否已加入
        Long alreadyJoined = participantMapper.selectCount(new LambdaQueryWrapper<MatchParticipant>()
                .eq(MatchParticipant::getActivityId, activityId)
                .eq(MatchParticipant::getUserId, userId)
                .eq(MatchParticipant::getPayStatus, 1));
        if (alreadyJoined > 0) {
            throw new BusinessException("您已经加入该拼场，无需重复上车");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getBalance().compareTo(act.getCostPerPerson()) < 0) {
            throw new BusinessException("钱包余额不足支付 AA 份额 (需 ￥" + act.getCostPerPerson() + "，当前 ￥" + user.getBalance() + ")，请先充值");
        }

        // 扣除用户 AA 费用
        user.setBalance(user.getBalance().subtract(act.getCostPerPerson()));
        userMapper.updateById(user);

        LocalDateTime now = LocalDateTime.now();
        MatchParticipant p = MatchParticipant.builder()
                .activityId(activityId)
                .userId(userId)
                .username(user.getUsername())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                .avatar(user.getAvatar())
                .payAmount(act.getCostPerPerson())
                .payStatus(1)
                .isCreator(0)
                .joinTime(now)
                .build();
        participantMapper.insert(p);

        act.setCurrentMembers(act.getCurrentMembers() + 1);
        act.setUpdateTime(now);

        // 满员成团判定！
        if (act.getCurrentMembers() >= act.getTargetMembers()) {
            act.setStatus(1); // 1-拼场成功已锁场出票

            // 查出该活动所有成员，批量分发独立 6 位专属核销码
            List<MatchParticipant> allP = participantMapper.selectList(new LambdaQueryWrapper<MatchParticipant>()
                    .eq(MatchParticipant::getActivityId, activityId)
                    .eq(MatchParticipant::getPayStatus, 1));

            String masterVerifyCode = null;
            for (MatchParticipant item : allP) {
                String code = String.format("%06d", new Random().nextInt(900000) + 100000);
                item.setVerifyCode(code);
                participantMapper.updateById(item);
                if (item.getIsCreator() == 1 || masterVerifyCode == null) {
                    masterVerifyCode = code;
                }
            }

            // 同步生成对应的全局正式预约订单 booking_order 供道闸与前台直接核验！
            String orderNo = "ORD" + act.getActivityNo();
            BookingOrder bookingOrder = BookingOrder.builder()
                    .orderNo(orderNo)
                    .userId(act.getCreatorId())
                    .venueId(act.getVenueId())
                    .bookDate(act.getBookDate())
                    .timeSlot(act.getTimeSlot())
                    .totalAmount(act.getTotalAmount())
                    .payStatus(1) // 已全额支付
                    .orderStatus(1) // 预约成功(待核销)
                    .verifyCode(masterVerifyCode)
                    .contactName(act.getTitle() + "(拼场成团)")
                    .contactPhone("13900000000")
                    .payTime(now)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            bookingOrderMapper.insert(bookingOrder);

            // WebSocket 全网广播：满员成团！
            webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                    .eventType("PAY")
                    .venueId(act.getVenueId())
                    .bookDate(act.getBookDate())
                    .timeSlot(act.getTimeSlot())
                    .status(2)
                    .userId(userId)
                    .message("🎉 恭喜！【" + act.getTitle() + "】拼场成功满员成团！各成员专属入场码已下发！")
                    .timestamp(System.currentTimeMillis())
                    .build());

            log.info("拼场满员成团: activityId={}, activityNo={}, masterVerifyCode={}", activityId, act.getActivityNo(), masterVerifyCode);
        } else {
            // 未满员，广播当前进度
            webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                    .eventType("LOCK")
                    .venueId(act.getVenueId())
                    .bookDate(act.getBookDate())
                    .timeSlot(act.getTimeSlot())
                    .status(1)
                    .userId(userId)
                    .message("球友 " + user.getNickname() + " 加入了拼场，当前进度 (" + act.getCurrentMembers() + "/" + act.getTargetMembers() + ")！")
                    .timestamp(System.currentTimeMillis())
                    .build());
        }

        updateById(act);
        return getMatchDetail(activityId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelMatch(Long activityId, Long userId) {
        MatchActivity act = getById(activityId);
        if (act == null) {
            throw new BusinessException("拼场活动不存在");
        }

        if (!act.getCreatorId().equals(userId)) {
            throw new BusinessException("只有发起人才能解散拼场");
        }

        if (act.getStatus() == 1 || act.getStatus() == 2) {
            throw new BusinessException("该活动已满员成团出票，如需取消请联系客服或前台退订");
        }

        if (act.getStatus() == 3) {
            throw new BusinessException("该活动已被解散");
        }

        act.setStatus(3); // 3-已解散退款
        act.setUpdateTime(LocalDateTime.now());
        updateById(act);

        // 原路退款给所有已支付成员
        List<MatchParticipant> participants = participantMapper.selectList(new LambdaQueryWrapper<MatchParticipant>()
                .eq(MatchParticipant::getActivityId, activityId)
                .eq(MatchParticipant::getPayStatus, 1));

        for (MatchParticipant p : participants) {
            SysUser u = userMapper.selectById(p.getUserId());
            if (u != null) {
                u.setBalance(u.getBalance().add(p.getPayAmount()));
                userMapper.updateById(u);
            }
            p.setPayStatus(2); // 已退款
            participantMapper.updateById(p);
        }

        // 解除时段锁
        String lockKey = "SLOT:LOCK:" + act.getVenueId() + ":" + act.getBookDate() + ":" + act.getTimeSlot();
        luaLockManager.unlockAtomic(lockKey, "FORCE_UNLOCK");

        // 广播释放时段
        webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                .eventType("CANCEL")
                .venueId(act.getVenueId())
                .bookDate(act.getBookDate())
                .timeSlot(act.getTimeSlot())
                .status(0)
                .userId(userId)
                .message("拼场【" + act.getTitle() + "】已解散，场地时段已恢复空闲")
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("发起人解散拼场并退款: activityId={}, refundMembersCount={}", activityId, participants.size());
    }

    @Override
    public List<MatchActivity> listMyMatches(Long userId) {
        List<MatchParticipant> myParts = participantMapper.selectList(new LambdaQueryWrapper<MatchParticipant>()
                .eq(MatchParticipant::getUserId, userId)
                .orderByDesc(MatchParticipant::getJoinTime));

        if (myParts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> actIds = myParts.stream().map(MatchParticipant::getActivityId).distinct().collect(Collectors.toList());
        List<MatchActivity> list = listByIds(actIds);

        Map<Long, MatchParticipant> myPartMap = myParts.stream()
                .collect(Collectors.toMap(MatchParticipant::getActivityId, p -> p, (k1, k2) -> k1));

        for (MatchActivity act : list) {
            MatchParticipant myP = myPartMap.get(act.getId());
            if (myP != null) {
                act.setIsJoined(true);
                act.setMyVerifyCode(myP.getVerifyCode());
            }
        }
        list.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return list;
    }
}
