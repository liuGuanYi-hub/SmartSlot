package com.smartslot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartslot.entity.OrderReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderReviewMapper extends BaseMapper<OrderReview> {

    @Select("SELECT r.*, " +
            "COALESCE(u.nickname, '资深球友达人') as user_nickname, " +
            "COALESCE(u.avatar, 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png') as user_avatar, " +
            "COALESCE(u.role, 'ROLE_USER') as user_role, " +
            "v.name as venue_name " +
            "FROM order_review r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN venue v ON r.venue_id = v.id " +
            "WHERE r.venue_id = #{venueId} " +
            "ORDER BY r.create_time DESC")
    List<OrderReview> selectReviewsByVenueId(@Param("venueId") Long venueId);

    @Update("UPDATE order_review SET likes = IFNULL(likes, 0) + 1 WHERE id = #{id}")
    int incrementLikes(@Param("id") Long id);
}
