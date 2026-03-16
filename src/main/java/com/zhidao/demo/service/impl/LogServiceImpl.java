package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.entity.SysLog;
import com.zhidao.demo.mapper.LogMapper;
import com.zhidao.demo.service.LogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, SysLog> implements LogService {
    @Override
    public void saveLog(Long userId, String username, String operation, String method, String params, Long time, String ip) {
        SysLog entry = new SysLog();
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setOperation(operation);
        entry.setMethod(method);
        entry.setParams(params);
        entry.setTime(time);
        entry.setIp(ip);
        entry.setCreateTime(LocalDateTime.now());
        save(entry);
    }
}
