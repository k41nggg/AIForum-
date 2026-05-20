package com.zhidao.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhidao.demo.dto.MessageVO;
import com.zhidao.demo.entity.SysMessage;

public interface SysMessageService extends IService<SysMessage> {

    IPage<MessageVO> listMessages(Long receiverId, int current, int size);

    long countUnread(Long receiverId);

    void markRead(Long receiverId, Long messageId);

    void markAllRead(Long receiverId);
}
