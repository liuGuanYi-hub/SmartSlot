package com.smartslot.service;

import java.util.concurrent.TimeUnit;

/**
 * 订单延时队列服务接口
 * 负责 15 分钟超时未支付订单精准秒级关单与时段库存自动释放
 */
public interface OrderDelayQueueService {

    /**
     * 投递订单超时延迟消息
     *
     * @param orderNo  业务订单号
     * @param delay    延时时长
     * @param timeUnit 时间单位 (如 TimeUnit.MINUTES)
     */
    void sendOrderTimeoutDelay(String orderNo, long delay, TimeUnit timeUnit);
}
