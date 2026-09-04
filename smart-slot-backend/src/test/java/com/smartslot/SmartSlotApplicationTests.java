package com.smartslot;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartslot.dto.BookingCreateDto;
import com.smartslot.entity.BookingOrder;
import com.smartslot.entity.Venue;
import com.smartslot.service.BookingOrderService;
import com.smartslot.service.VenueService;
import com.smartslot.util.LockManager;
import com.smartslot.vo.SlotMatrixVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class SmartSlotApplicationTests {

    @Autowired
    private VenueService venueService;

    @Autowired
    private BookingOrderService bookingOrderService;

    @Autowired
    private LockManager lockManager;

    @Test
    @DisplayName("测试系统上下文与数据库连通性")
    void contextLoads() {
        Assertions.assertNotNull(venueService);
        Assertions.assertNotNull(bookingOrderService);
    }

    @Test
    @DisplayName("测试 MyBatis-Plus 分页插件功能")
    void testMyBatisPlusPagination() {
        Page<Venue> page = venueService.pageVenues(new Page<>(1, 3), null, null);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getTotal() > 0, "场地总数应大于 0");
        Assertions.assertTrue(page.getRecords().size() <= 3, "单页记录数不超过 3");
    }

    @Test
    @DisplayName("测试日历时段矩阵看板数据生成")
    void testSlotMatrixGeneration() {
        SlotMatrixVo matrix = bookingOrderService.getSlotMatrix(LocalDate.now(), null, 1L);
        Assertions.assertNotNull(matrix);
        Assertions.assertFalse(matrix.getVenues().isEmpty(), "场地列表不应为空");
        Assertions.assertEquals(13, matrix.getTimeSlots().size(), "应包含 09:00~22:00 的 13 个标准时段");
    }

    @Test
    @DisplayName("测试时段防超卖锁机制 (双模降级保障)")
    void testLockMechanism() {
        String testKey = "test:slot:lock:1:2026-09-05:09:00-10:00";
        // 第一次加锁应成功
        boolean lock1 = lockManager.tryLock(testKey, "USER_A", 10);
        Assertions.assertTrue(lock1, "首次加锁应成功");

        // 重复加锁应被互斥拒绝 (防超卖)
        boolean lock2 = lockManager.tryLock(testKey, "USER_B", 10);
        Assertions.assertFalse(lock2, "已被占用的时段重复加锁应失败");

        // 释放锁
        lockManager.unlock(testKey);
        boolean isLockedAfterUnlock = lockManager.isLocked(testKey);
        Assertions.assertFalse(isLockedAfterUnlock, "解锁后状态应为空闲");
    }
}
