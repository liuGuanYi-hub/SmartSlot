package com.smartslot.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 日历时段网格矩阵 VO
 * 支撑核心亮点：日历时段矩阵组件化展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotMatrixVo {

    private String date; // 格式: YYYY-MM-DD
    private List<String> timeSlots; // 所有时段列表
    private List<VenueColumnVo> venues; // 各场地及对应时段状态

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueColumnVo {
        private Long venueId;
        private String venueName;
        private String categoryName;
        private BigDecimal pricePerHour;
        private Integer venueStatus; // 1-开放, 0-维护
        private List<SlotItemVo> slots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotItemVo {
        private String timeSlot;
        // 0: 空闲可选, 1: 待支付锁定中(倒计时), 2: 已预约(不可选), 3: 场地维护不可约
        private Integer status;
        private Boolean isMine; // 是否为当前登录用户的预约
        private String orderNo;
        private String verifyCode;
    }
}
