package com.smartslot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartslot.entity.OrderReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderReviewMapper extends BaseMapper<OrderReview> {

    @Select("SELECT r.*, u.nickname as user_nickname, u.avatar as user_avatar " +
            "FROM order_review r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.venue_id = #{venueId} " +
            "ORDER BY r.create_time DESC")
    List<OrderReview> selectReviewsByVenueId(@Param("venueId") Long venueId);
}
