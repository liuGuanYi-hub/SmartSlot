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

    @Autowired
    private com.smartslot.util.LuaLockManager luaLockManager;

    @Autowired
    private com.smartslot.service.OrderDelayQueueService orderDelayQueueService;

    @Test
    @DisplayName("测试系统上下文与数据库连通性")
    void contextLoads() {
        Assertions.assertNotNull(venueService);
        Assertions.assertNotNull(bookingOrderService);
        Assertions.assertNotNull(luaLockManager);
        Assertions.assertNotNull(orderDelayQueueService);
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
    @DisplayName("测试旧版内存锁管理器功能 (兜底保留)")
    void testLegacyLockMechanism() {
        String testKey = "test:slot:lock:1:2026-09-05:09:00-10:00";
        boolean lock1 = lockManager.tryLock(testKey, "USER_A", 10);
        Assertions.assertTrue(lock1, "首次加锁应成功");
        boolean lock2 = lockManager.tryLock(testKey, "USER_B", 10);
        Assertions.assertFalse(lock2, "已被占用的时段重复加锁应失败");
        lockManager.unlock(testKey);
        boolean isLockedAfterUnlock = lockManager.isLocked(testKey);
        Assertions.assertFalse(isLockedAfterUnlock, "解锁后状态应为空闲");
    }

    @Test
    @DisplayName("测试企业级 Redis Lua 原子时段锁 (抢占/互斥/凭据防误删)")
    void testLuaLockMechanism() {
        String testKey = "test:lua:lock:1:2026-09-06:14:00-15:00";
        String userA = "UID:1001";
        String userB = "UID:1002";

        // 1. 用户A 首次抢锁应成功
        boolean lockA = luaLockManager.tryLockAtomic(testKey, userA, 10);
        Assertions.assertTrue(lockA, "用户A 首次抢占锁应成功");

        // 2. 用户B 抢相同锁应被互斥拒绝 (防超卖)
        boolean lockB = luaLockManager.tryLockAtomic(testKey, userB, 10);
        Assertions.assertFalse(lockB, "用户B 抢占已被占用的锁应失败");

        // 3. 用户B 试图释放用户A 的锁，应因凭据不匹配被拒绝
        boolean unlockB = luaLockManager.unlockAtomic(testKey, userB);
        Assertions.assertFalse(unlockB, "非持有者凭据释放锁应被拒绝");

        // 4. 用户A 凭自身凭据释放锁成功
        boolean unlockA = luaLockManager.unlockAtomic(testKey, userA);
        Assertions.assertTrue(unlockA, "持有着凭据释放锁应成功");

        // 5. 解锁后应恢复空闲
        Assertions.assertFalse(luaLockManager.isLocked(testKey), "释放后时段应为空闲");
    }

    @Test
    @DisplayName("测试延时队列与超时关单联动 (Redisson/JVM 双模)")
    void testOrderDelayQueueTimeoutCancel() throws InterruptedException {
        // 创建一个测试待支付订单
        BookingCreateDto dto = new BookingCreateDto();
        dto.setVenueId(1L);
        dto.setBookDate(LocalDate.now().plusDays(2));
        dto.setTimeSlot("21:00-22:00");
        dto.setContactName("延时测试员");
        dto.setContactPhone("13900008888");

        BookingOrder order = bookingOrderService.lockAndCreateOrder(dto, 1L);
        Assertions.assertNotNull(order);
        Assertions.assertEquals(0, order.getOrderStatus(), "新建订单状态应为待支付(0)");

        // 投递 1 秒后超时取消任务
        orderDelayQueueService.sendOrderTimeoutDelay(order.getOrderNo(), 1, java.util.concurrent.TimeUnit.SECONDS);

        // 等待延时任务消费执行
        Thread.sleep(1800);

        // 再次查询该订单，应已被自动取消
        BookingOrder updatedOrder = bookingOrderService.getById(order.getId());
        Assertions.assertNotNull(updatedOrder);
        Assertions.assertEquals(3, updatedOrder.getOrderStatus(), "超时后订单状态应变为已取消(3)");
    }
}
