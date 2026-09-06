package com.smartslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 时段实时状态变更广播事件传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotEventDto {

    /**
     * 事件类型:
     * LOCK: 抢占锁定中 (status=1)
     * PAY: 支付成功 (status=2)
     * CANCEL: 自主取消释放 (status=0)
     * TIMEOUT: 延时超时释放 (status=0)
     * ONLINE_COUNT: 在线人数变动
     */
    private String eventType;

    private Long venueId;
    private String venueName;
    private LocalDate bookDate;
    private String timeSlot;
    private Integer status; // 0:空闲, 1:待支付锁定, 2:已预约, 3:维护
    private Long userId;
    private Integer onlineCount;
    private String message;
    private String extra; // 扩展报文(如 IoT 闸机指令或网关通知)
    private Long timestamp;
}
