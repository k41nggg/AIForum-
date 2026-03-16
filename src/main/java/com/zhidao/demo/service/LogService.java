package com.zhidao.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhidao.demo.entity.SysLog;

public interface LogService extends IService<SysLog> {
    void saveLog(Long userId, String username, String operation, String method, String params, Long time, String ip);
}
