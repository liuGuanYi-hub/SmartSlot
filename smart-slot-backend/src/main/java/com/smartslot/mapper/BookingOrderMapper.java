package com.smartslot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartslot.entity.BookingOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookingOrderMapper extends BaseMapper<BookingOrder> {

    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM booking_order WHERE pay_status = 1")
    BigDecimal sumTotalRevenue();

    @Select("SELECT DATE(pay_time) as dateStr, IFNULL(SUM(total_amount), 0) as amount, COUNT(id) as orderCount " +
            "FROM booking_order " +
            "WHERE pay_status = 1 AND pay_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(pay_time) " +
            "ORDER BY dateStr ASC")
    List<Map<String, Object>> selectRecentRevenueTrend();

    @Select("SELECT v.name as name, COUNT(b.id) as value " +
            "FROM booking_order b " +
            "LEFT JOIN venue v ON b.venue_id = v.id " +
            "WHERE b.order_status IN (1, 2) " +
            "GROUP BY b.venue_id, v.name " +
            "ORDER BY value DESC LIMIT 6")
    List<Map<String, Object>> selectVenuePopularity();
}
