package com.smartslot.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVo {

    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Long totalUsers;
    private Long todayBookings;

    // 近7天日期列表: ["08-29", "08-30", ...]
    private List<String> trendDates;
    // 近7天销售额: [120.00, 350.00, ...]
    private List<BigDecimal> trendRevenues;
    // 近7天预约单量: [2, 6, ...]
    private List<Long> trendOrderCounts;

    // 各场地预约热度占比: [{"name": "羽毛球1号馆", "value": 15}, ...]
    private List<Map<String, Object>> venuePopularity;
}
