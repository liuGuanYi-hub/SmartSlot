package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartslot.entity.BookingOrder;
import com.smartslot.entity.SysUser;
import com.smartslot.mapper.BookingOrderMapper;
import com.smartslot.mapper.SysUserMapper;
import com.smartslot.service.DashboardService;
import com.smartslot.vo.DashboardVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingOrderMapper orderMapper;
    private final SysUserMapper userMapper;

    @Override
    public DashboardVo getDashboardStats() {
        Long totalOrders = orderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .in(BookingOrder::getOrderStatus, 1, 2));

        BigDecimal totalRevenue = orderMapper.sumTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Long totalUsers = userMapper.selectCount(null);

        Long todayBookings = orderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getBookDate, LocalDate.now())
                .in(BookingOrder::getOrderStatus, 1, 2));

        // 构造近 7 天趋势骨架
        List<String> trendDates = new ArrayList<>();
        List<BigDecimal> trendRevenues = new ArrayList<>();
        List<Long> trendOrderCounts = new ArrayList<>();

        Map<String, BigDecimal> revMap = new HashMap<>();
        Map<String, Long> countMap = new HashMap<>();

        List<Map<String, Object>> rawTrend = orderMapper.selectRecentRevenueTrend();
        if (rawTrend != null) {
            for (Map<String, Object> map : rawTrend) {
                Object d = map.get("dateStr");
                if (d != null) {
                    String dateKey = d.toString();
                    BigDecimal amt = (BigDecimal) map.get("amount");
                    Long cnt = ((Number) map.get("orderCount")).longValue();
                    revMap.put(dateKey, amt);
                    countMap.put(dateKey, cnt);
                }
            }
        }

        LocalDate now = LocalDate.now();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter displayDf = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate d = now.minusDays(i);
            String fullDate = d.format(df);
            trendDates.add(d.format(displayDf));
            trendRevenues.add(revMap.getOrDefault(fullDate, BigDecimal.ZERO));
            trendOrderCounts.add(countMap.getOrDefault(fullDate, 0L));
        }

        List<Map<String, Object>> venuePopularity = orderMapper.selectVenuePopularity();
        if (venuePopularity == null || venuePopularity.isEmpty()) {
            venuePopularity = List.of(
                    Map.of("name", "羽毛球1号馆", "value", 12),
                    Map.of("name", "网球中心1号", "value", 8),
                    Map.of("name", "篮球半场A", "value", 5)
            );
        }

        return DashboardVo.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalUsers(totalUsers)
                .todayBookings(todayBookings)
                .trendDates(trendDates)
                .trendRevenues(trendRevenues)
                .trendOrderCounts(trendOrderCounts)
                .venuePopularity(venuePopularity)
                .build();
    }
}
