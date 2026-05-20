package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.dto.MessageVO;
import com.zhidao.demo.entity.SysMessage;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.mapper.SysMessageMapper;
import com.zhidao.demo.mapper.UserMapper;
import com.zhidao.demo.service.SysMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysMessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements SysMessageService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public IPage<MessageVO> listMessages(Long receiverId, int current, int size) {
        Page<SysMessage> page = new Page<>(current, size);
        IPage<SysMessage> raw = page(page, new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, receiverId)
                .orderByDesc(SysMessage::getCreateTime));

        List<Long> senderIds = raw.getRecords().stream()
                .map(SysMessage::getSenderId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = senderIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(senderIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return raw.convert(m -> toVO(m, userMap));
    }

    @Override
    public long countUnread(Long receiverId) {
        return count(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getIsRead, 0));
    }

    @Override
    public void markRead(Long receiverId, Long messageId) {
        update(new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getId, messageId)
                .eq(SysMessage::getReceiverId, receiverId)
                .set(SysMessage::getIsRead, 1));
    }

    @Override
    public void markAllRead(Long receiverId) {
        update(new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getIsRead, 0)
                .set(SysMessage::getIsRead, 1));
    }

    private MessageVO toVO(SysMessage m, Map<Long, User> userMap) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setType(m.getType());
        vo.setTitle(m.getTitle());
        vo.setContent(m.getContent());
        vo.setSenderId(m.getSenderId());
        vo.setTargetType(m.getTargetType());
        vo.setTargetId(m.getTargetId());
        vo.setExtraId(m.getExtraId());
        vo.setIsRead(m.getIsRead());
        vo.setCreateTime(m.getCreateTime());
        if (m.getSenderId() != null && m.getSenderId() > 0) {
            User u = userMap.get(m.getSenderId());
            if (u != null) {
                vo.setSenderNickname(u.getNickname());
                vo.setSenderAvatar(u.getAvatar());
            }
        }
        return vo;
    }
}
