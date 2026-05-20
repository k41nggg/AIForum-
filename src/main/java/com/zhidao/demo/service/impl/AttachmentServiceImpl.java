package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.config.UploadProperties;
import com.zhidao.demo.dto.UploadResponse;
import com.zhidao.demo.entity.ForumAttachment;
import com.zhidao.demo.mapper.AttachmentMapper;
import com.zhidao.demo.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, ForumAttachment>
        implements AttachmentService {

    @Autowired
    private UploadProperties uploadProperties;

    @Override
    public UploadResponse uploadImage(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !uploadProperties.allowedTypeSet().contains(contentType)) {
            throw new IllegalArgumentException("仅支持 JPEG/PNG/GIF/WebP 图片");
        }
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new IllegalArgumentException("图片大小不能超过 " + (uploadProperties.getMaxSize() / 1024 / 1024) + "MB");
        }

        String ext = extensionFromMime(contentType);
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = dateDir + "/" + storedName;

        Path root = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("保存文件失败", e);
        }

        Integer width = null;
        Integer height = null;
        try {
            BufferedImage img = ImageIO.read(target.toFile());
            if (img != null) {
                width = img.getWidth();
                height = img.getHeight();
            }
        } catch (IOException ignored) {
            // 尺寸可选
        }

        String fileUrl = "/uploads/" + relativePath.replace("\\", "/");
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : storedName;

        ForumAttachment att = new ForumAttachment();
        att.setUserId(userId);
        att.setBizType(ForumAttachment.BIZ_TYPE_POST);
        att.setFileName(originalName);
        att.setFilePath(relativePath.replace("\\", "/"));
        att.setFileUrl(fileUrl);
        att.setMimeType(contentType);
        att.setFileSize(file.getSize());
        att.setWidth(width);
        att.setHeight(height);
        att.setStatus(ForumAttachment.STATUS_UPLOADED);
        save(att);

        return new UploadResponse(att.getId(), att.getFileUrl(), att.getFileName());
    }

    @Override
    public void bindToPost(List<Long> attachmentIds, Long postId, Long userId) {
        if (attachmentIds == null || attachmentIds.isEmpty() || postId == null || userId == null) {
            return;
        }
        update(new LambdaUpdateWrapper<ForumAttachment>()
                .in(ForumAttachment::getId, attachmentIds)
                .eq(ForumAttachment::getUserId, userId)
                .eq(ForumAttachment::getStatus, ForumAttachment.STATUS_UPLOADED)
                .eq(ForumAttachment::getBizType, ForumAttachment.BIZ_TYPE_POST)
                .set(ForumAttachment::getPostId, postId)
                .set(ForumAttachment::getStatus, ForumAttachment.STATUS_BOUND));
    }

    private static String extensionFromMime(String mime) {
        return switch (mime) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
