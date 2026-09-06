package com.smartslot.task;

import com.smartslot.service.BookingOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单与时段全链路状态自愈对账定时任务
 * 兜底解决因宕机、重启或网络异常未消费 Redisson 延迟队列而产生的僵死待支付订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReconciliationTask {

    private final BookingOrderService bookingOrderService;

    /**
     * 每 60 秒自动巡检一次超时未付订单并执行状态自愈
     */
    @Scheduled(fixedDelay = 60000)
    public void executeOrderReconciliation() {
        try {
            int healed = bookingOrderService.reconcileExpiredOrders();
            if (healed > 0) {
                log.info("[定时巡检] 发现并成功自愈 {} 笔僵死超时订单", healed);
            }
        } catch (Exception e) {
            log.error("[定时巡检] 巡检自愈执行异常", e);
        }
    }
}
