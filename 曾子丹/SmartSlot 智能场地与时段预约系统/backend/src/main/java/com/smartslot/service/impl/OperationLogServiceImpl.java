package com.smartslot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.entity.OperationLog;
import com.smartslot.mapper.OperationLogMapper;
import com.smartslot.service.OperationLogService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
}
