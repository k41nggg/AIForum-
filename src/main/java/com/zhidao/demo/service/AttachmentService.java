package com.zhidao.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhidao.demo.dto.UploadResponse;
import com.zhidao.demo.entity.ForumAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService extends IService<ForumAttachment> {

    UploadResponse uploadImage(Long userId, MultipartFile file);

    void bindToPost(List<Long> attachmentIds, Long postId, Long userId);
}
