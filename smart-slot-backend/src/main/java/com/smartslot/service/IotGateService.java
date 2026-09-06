package com.smartslot.service;

import com.smartslot.entity.BookingOrder;
import com.smartslot.entity.IotGateLog;

import java.util.List;
import java.util.Map;

public interface IotGateService {

    IotGateLog sendGateOpenCommand(BookingOrder order);

    IotGateLog manualControlGate(String gateId, String action, String operator);

    List<IotGateLog> getRecentLogs(int limit);

    List<Map<String, Object>> getGateDevices();
}
