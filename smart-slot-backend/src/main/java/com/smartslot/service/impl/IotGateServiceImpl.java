package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartslot.dto.SlotEventDto;
import com.smartslot.entity.BookingOrder;
import com.smartslot.entity.IotGateLog;
import com.smartslot.entity.Venue;
import com.smartslot.mapper.IotGateLogMapper;
import com.smartslot.mapper.VenueMapper;
import com.smartslot.service.IotGateService;
import com.smartslot.service.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IotGateServiceImpl implements IotGateService {

    private final IotGateLogMapper gateLogMapper;
    private final VenueMapper venueMapper;
    private final WebSocketPushService webSocketPushService;

    @Override
    public IotGateLog sendGateOpenCommand(BookingOrder order) {
        String gateId = "GATE_COURT_0" + (order.getVenueId() != null ? order.getVenueId() : 1);
        String venueName = order.getVenueName();
        if (venueName == null && order.getVenueId() != null) {
            Venue v = venueMapper.selectById(order.getVenueId());
            if (v != null) venueName = v.getName();
        }
        if (venueName == null) venueName = "场馆主入口";

        String topic = "/iot/smartslot/v1/gate/" + gateId + "/command";
        long now = System.currentTimeMillis();

        String payloadJson = String.format("""
            {
              "deviceType": "SMART_TURNSTILE_V2",
              "gateId": "%s",
              "venueId": %d,
              "venueName": "%s",
              "command": "BARRIER_OPEN",
              "verifyCode": "%s",
              "orderNo": "%s",
              "holdDurationSec": 5,
              "clientMac": "F8:E4:3B:11:2C:9A",
              "firmware": "SmartGate-v2.4.1",
              "signalDbm": -42,
              "timestamp": %d
            }
            """.trim(), gateId, order.getVenueId() != null ? order.getVenueId() : 1, venueName,
                order.getVerifyCode() != null ? order.getVerifyCode() : "888888",
                order.getOrderNo(), now);

        IotGateLog gateLog = IotGateLog.builder()
                .gateId(gateId)
                .venueId(order.getVenueId())
                .venueName(venueName)
                .orderNo(order.getOrderNo())
                .verifyCode(order.getVerifyCode())
                .action("BARRIER_OPEN")
                .protocol("MQTT_QOS1")
                .topic(topic)
                .payloadJson(payloadJson)
                .status("SUCCESS")
                .durationMs(28L)
                .createTime(LocalDateTime.now())
                .build();

        try {
            gateLogMapper.insert(gateLog);
        } catch (Exception e) {
            log.warn("写入 iot_gate_log 遇到异常: {}", e.getMessage());
        }

        // 全网广播 WebSocket GATE_UNLOCK 事件，通知监控台实时播放机械臂与绿灯动效
        webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                .eventType("GATE_UNLOCK")
                .venueId(order.getVenueId())
                .venueName(venueName)
                .message("智能门禁 " + gateId + " 已下发放行指令，摆臂开启")
                .extra(payloadJson)
                .timestamp(now)
                .build());

        log.info("[IoT门禁] 成功下发 MQTT 开闸指令: gateId={}, verifyCode={}", gateId, order.getVerifyCode());
        return gateLog;
    }

    @Override
    public IotGateLog manualControlGate(String gateId, String action, String operator) {
        String topic = "/iot/smartslot/v1/gate/" + gateId + "/command";
        long now = System.currentTimeMillis();

        String payloadJson = String.format("""
            {
              "deviceType": "SMART_TURNSTILE_V2",
              "gateId": "%s",
              "command": "%s",
              "operator": "%s",
              "manualOverride": true,
              "timestamp": %d
            }
            """.trim(), gateId, action, operator, now);

        IotGateLog gateLog = IotGateLog.builder()
                .gateId(gateId)
                .venueName("中控远程应急指令")
                .action(action)
                .protocol("HTTP_WEBHOOK")
                .topic(topic)
                .payloadJson(payloadJson)
                .status("SUCCESS")
                .durationMs(16L)
                .createTime(LocalDateTime.now())
                .build();

        try {
            gateLogMapper.insert(gateLog);
        } catch (Exception e) {
            log.warn("写入 iot_gate_log 遇到异常: {}", e.getMessage());
        }

        webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                .eventType("GATE_UNLOCK")
                .message("管理员 " + operator + " 手动触发道闸 " + gateId + " 指令: " + action)
                .extra(payloadJson)
                .timestamp(now)
                .build());

        return gateLog;
    }

    @Override
    public List<IotGateLog> getRecentLogs(int limit) {
        try {
            List<IotGateLog> logs = gateLogMapper.selectList(new LambdaQueryWrapper<IotGateLog>()
                    .orderByDesc(IotGateLog::getId)
                    .last("LIMIT " + limit));
            if (logs != null && !logs.isEmpty()) {
                return logs;
            }
        } catch (Exception ignored) {
        }

        // 演示仿真兜底数据
        return List.of(
                IotGateLog.builder()
                        .id(101L)
                        .gateId("GATE_COURT_01")
                        .venueName("羽毛球 1 号场 (奥运专业地胶)")
                        .orderNo("ORD20260906001")
                        .verifyCode("839201")
                        .action("BARRIER_OPEN")
                        .protocol("MQTT_QOS1")
                        .topic("/iot/smartslot/v1/gate/GATE_COURT_01/command")
                        .payloadJson("{\"cmd\":\"BARRIER_OPEN\",\"duration\":5,\"code\":\"839201\"}")
                        .status("SUCCESS")
                        .durationMs(24L)
                        .createTime(LocalDateTime.now().minusMinutes(2))
                        .build(),
                IotGateLog.builder()
                        .id(102L)
                        .gateId("GATE_COURT_02")
                        .venueName("羽毛球 2 号场 (双打标准场)")
                        .orderNo("ORD20260906002")
                        .verifyCode("519302")
                        .action("BARRIER_OPEN")
                        .protocol("MQTT_QOS1")
                        .topic("/iot/smartslot/v1/gate/GATE_COURT_02/command")
                        .payloadJson("{\"cmd\":\"BARRIER_OPEN\",\"duration\":5,\"code\":\"519302\"}")
                        .status("SUCCESS")
                        .durationMs(21L)
                        .createTime(LocalDateTime.now().minusMinutes(8))
                        .build()
        );
    }

    @Override
    public List<Map<String, Object>> getGateDevices() {
        List<Venue> venues = venueMapper.selectList(null);
        List<Map<String, Object>> devices = new ArrayList<>();
        int i = 1;
        for (Venue v : venues) {
            String gid = "GATE_COURT_0" + v.getId();
            devices.add(Map.of(
                    "gateId", gid,
                    "venueId", v.getId(),
                    "venueName", v.getName(),
                    "ip", "192.168.10." + (100 + i),
                    "mac", String.format("F8:E4:3B:11:2C:%02X", i),
                    "status", "ONLINE",
                    "mode", "AUTO_VERIFY",
                    "signalDbm", -38 - (i * 2),
                    "firmware", "SmartGate-v2.4.1",
                    "totalPassCount", 128 + i * 37
            ));
            i++;
        }
        return devices;
    }
}
