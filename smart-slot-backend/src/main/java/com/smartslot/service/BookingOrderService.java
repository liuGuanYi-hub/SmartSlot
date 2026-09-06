package com.smartslot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartslot.dto.BookingCreateDto;
import com.smartslot.dto.ReviewCreateDto;
import com.smartslot.entity.BookingOrder;
import com.smartslot.vo.SlotMatrixVo;

import java.time.LocalDate;

public interface BookingOrderService extends IService<BookingOrder> {

    SlotMatrixVo getSlotMatrix(LocalDate bookDate, Long categoryId, Long currentUserId);

    BookingOrder lockAndCreateOrder(BookingCreateDto dto, Long userId);

    BookingOrder payOrder(String orderNo, Long userId);

    void cancelOrder(Long orderId, Long userId, String reason);

    BookingOrder verifyOrder(String verifyCode);

    Page<BookingOrder> pageUserOrders(Page<BookingOrder> page, Long userId, Integer status);

    Page<BookingOrder> pageAdminOrders(Page<BookingOrder> page, String orderNo, String phone, Integer status);

    void addReview(ReviewCreateDto dto, Long userId);

    void handleTimeoutOrder(String orderNo);
}
