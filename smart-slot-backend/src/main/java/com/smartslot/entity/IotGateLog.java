package com.smartslot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("iot_gate_log")
public class IotGateLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String gateId;        // 闸机硬件编号，如 GATE_COURT_01

    private Long venueId;         // 所属场馆ID

    private String venueName;     // 场馆名称

    private String orderNo;       // 关联预约单号

    private String verifyCode;    // 触发开闸的6位核销码

    private String action;        // 操作指令: BARRIER_OPEN, BARRIER_CLOSE, EMERGENCY_OPEN

    private String protocol;      // 通讯协议: MQTT_QOS1, HTTP_WEBHOOK

    private String topic;         // MQTT 主题，如 /iot/smartslot/v1/gate/GATE_01/command

    private String payloadJson;   // 下发的原始 JSON 报文

    private String status;        // 执行状态: SUCCESS, OFFLINE_RETRY

    private Long durationMs;      // 通讯与舵机响应耗时(毫秒)

    private LocalDateTime createTime; // 下发时间
}
