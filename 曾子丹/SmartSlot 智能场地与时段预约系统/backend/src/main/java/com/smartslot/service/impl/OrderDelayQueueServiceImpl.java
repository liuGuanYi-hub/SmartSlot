package com.smartslot.service.impl;

import com.smartslot.service.BookingOrderService;
import com.smartslot.service.OrderDelayQueueService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * 订单延时队列核心实现类 (Redisson + JVM DelayQueue 双模容灾)
 * 1. 优先使用 Redisson RDelayedQueue 实现分布式精准延时消息
 * 2. 若 Redis 离线，自动无缝切换为基于 JVM DelayQueue 的本地延时调度
 */
@Slf4j
@Service
public class OrderDelayQueueServiceImpl implements OrderDelayQueueService {

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Autowired
    @Lazy
    private BookingOrderService bookingOrderService;

    private static final String QUEUE_NAME = "smartslot:order:delay:queue";
    private RBlockingQueue<String> redissonBlockingQueue;
    private RDelayedQueue<String> redissonDelayedQueue;

    // 本地 JVM 延时队列降级容灾
    private final DelayQueue<DelayedOrderTask> localDelayQueue = new DelayQueue<>();

    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "order-delay-consumer");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        if (redissonClient != null) {
            try {
                redissonBlockingQueue = redissonClient.getBlockingQueue(QUEUE_NAME);
                redissonDelayedQueue = redissonClient.getDelayedQueue(redissonBlockingQueue);
                log.info("Redisson 延迟队列初始化成功: {}", QUEUE_NAME);
            } catch (Exception e) {
                log.warn("Redisson 延迟队列初始化异常，启用本地 JVM DelayQueue 模式: {}", e.getMessage());
                redissonClient = null;
            }
        }

        // 启动后台守护消费者线程
        consumerExecutor.submit(this::listenAndConsume);
    }

    @Override
    public void sendOrderTimeoutDelay(String orderNo, long delay, TimeUnit timeUnit) {
        if (redissonClient != null && redissonDelayedQueue != null) {
            try {
                redissonDelayedQueue.offer(orderNo, delay, timeUnit);
                log.info("[Redisson延时队列] 成功投递订单超时关单任务: orderNo={}, 延时={} {}", orderNo, delay, timeUnit);
                return;
            } catch (Exception e) {
                log.warn("Redisson 延时队列投递失败，转入本地 JVM 延时队列: {}", e.getMessage());
            }
        }

        // 本地 JVM 延迟队列容灾降级
        long delayMillis = timeUnit.toMillis(delay);
        localDelayQueue.offer(new DelayedOrderTask(orderNo, delayMillis));
        log.info("[JVM延时队列] 本地投递订单超时关单任务: orderNo={}, 延时={}毫秒", orderNo, delayMillis);
    }

    private void listenAndConsume() {
        while (running) {
            try {
                String expiredOrderNo = null;

                // 1. 如果 Redisson 可用，优先从 Redisson 阻塞队列中获取已到期消息
                if (redissonClient != null && redissonBlockingQueue != null) {
                    try {
                        expiredOrderNo = redissonBlockingQueue.poll(500, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        // 网络异常时短暂退避
                        Thread.sleep(500);
                    }
                }

                // 2. 同时检查本地 JVM 延时队列中是否有到期消息
                if (expiredOrderNo == null) {
                    DelayedOrderTask localTask = localDelayQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (localTask != null) {
                        expiredOrderNo = localTask.getOrderNo();
                    }
                }

                // 3. 处理到期关单任务
                if (expiredOrderNo != null) {
                    log.info(">>> 收到订单超时到期信号: orderNo={}, 正在执行关单与库存释放...", expiredOrderNo);
                    bookingOrderService.handleTimeoutOrder(expiredOrderNo);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费延时订单消息异常: ", e);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        running = false;
        consumerExecutor.shutdownNow();
        if (redissonDelayedQueue != null) {
            redissonDelayedQueue.destroy();
        }
    }

    /**
     * JVM 本地延时任务包装类
     */
    @Getter
    private static class DelayedOrderTask implements Delayed {
        private final String orderNo;
        private final long executeTime;

        public DelayedOrderTask(String orderNo, long delayMillis) {
            this.orderNo = orderNo;
            this.executeTime = System.currentTimeMillis() + delayMillis;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(executeTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.executeTime, ((DelayedOrderTask) o).executeTime);
        }
    }
}
