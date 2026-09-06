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

    @Autowired
    private com.smartslot.service.WebSocketPushService webSocketPushService;

    @Autowired
    private com.smartslot.service.IdempotentTokenService idempotentTokenService;

    @Test
    @DisplayName("测试系统上下文与数据库连通性")
    void contextLoads() {
        Assertions.assertNotNull(venueService);
        Assertions.assertNotNull(bookingOrderService);
        Assertions.assertNotNull(luaLockManager);
        Assertions.assertNotNull(orderDelayQueueService);
        Assertions.assertNotNull(webSocketPushService);
        Assertions.assertNotNull(idempotentTokenService);
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

    @Test
    @DisplayName("测试 WebSocket 时段状态变更广播与在线人数统计")
    void testWebSocketBroadcast() {
        Assertions.assertDoesNotThrow(() -> {
            webSocketPushService.broadcastSlotChange(com.smartslot.dto.SlotEventDto.builder()
                    .eventType("LOCK")
                    .venueId(1L)
                    .venueName("羽毛球1号场")
                    .bookDate(LocalDate.now())
                    .timeSlot("10:00-11:00")
                    .status(1)
                    .userId(1L)
                    .message("测试广播消息")
                    .timestamp(System.currentTimeMillis())
                    .build());
            webSocketPushService.broadcastOnlineCount();
        });
        Assertions.assertTrue(webSocketPushService.getOnlineCount() >= 0);
    }

    @Test
    @DisplayName("测试接口幂等性 Token 机制 (生成/首次消费成功/重复消费拒绝)")
    void testIdempotentTokenMechanism() {
        String token = idempotentTokenService.generateToken();
        Assertions.assertNotNull(token);
        Assertions.assertTrue(token.startsWith("IDEMP_"));

        // 首次消费必须成功
        boolean firstConsume = idempotentTokenService.verifyAndConsume(token);
        Assertions.assertTrue(firstConsume, "首次消费幂等 Token 应成功");

        // 第二次重复消费必须被拦截
        boolean secondConsume = idempotentTokenService.verifyAndConsume(token);
        Assertions.assertFalse(secondConsume, "重复使用已消费的 Token 必须被拒绝拦截");

        // 伪造非法 Token 必须被拦截
        boolean fakeConsume = idempotentTokenService.verifyAndConsume("FAKE_TOKEN_XYZ");
        Assertions.assertFalse(fakeConsume, "伪造 Token 必须被拒绝拦截");
    }

    @Test
    @DisplayName("Day 6-7 并发压力测试: 30 线程瞬时并发争抢同一时段 (绝对零超卖验证)")
    void testHighConcurrencyAntiOverselling() throws InterruptedException {
        int threadCount = 30;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);

        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);

        LocalDate targetDate = LocalDate.now().plusDays(10);
        String targetSlot = "18:00-19:00";
        Long venueId = 1L;

        for (int i = 0; i < threadCount; i++) {
            final long uid = 2000L + i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // 等待所有线程就绪，发令枪响齐发
                    BookingCreateDto dto = new BookingCreateDto();
                    dto.setVenueId(venueId);
                    dto.setBookDate(targetDate);
                    dto.setTimeSlot(targetSlot);
                    dto.setContactName("并发测试员_" + uid);
                    dto.setContactPhone("13800138000");

                    bookingOrderService.lockAndCreateOrder(dto, uid);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // 30 线程瞬时并发齐发！
        doneLatch.await();
        executor.shutdown();

        // 验证断言: 必须且仅有 1 个线程抢购成功，其余全部被互斥拒绝！
        Assertions.assertEquals(1, successCount.get(), "高并发冲击下必须有且仅有 1 笔成功订单，杜绝超卖");
        Assertions.assertEquals(threadCount - 1, failCount.get(), "其余所有并发请求必须被互斥拒绝");
    }
}
