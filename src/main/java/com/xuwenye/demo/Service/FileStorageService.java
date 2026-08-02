package com.xuwenye.demo.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务：负责头像等上传文件的落盘保存与访问路径生成
 */
@Service
public class FileStorageService {

    /** 允许的图片扩展名白名单 */
    private static final List<String> ALLOWED_IMAGE_EXT = List.of("jpg", "jpeg", "png", "gif", "webp");

    /** 头像最大体积：2MB */
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    /** 头像子目录（相对 upload 根目录） */
    private static final String AVATAR_SUB_DIR = "avatars";

    /**
     * 保存头像文件，返回可访问的 URL 路径
     *
     * @param file 上传的图片文件
     * @return 例如 /uploads/avatars/xxx.png
     * @throws IllegalArgumentException 文件为空 / 类型不允许 / 超过大小限制
     */
    public String storeAvatar(MultipartFile file) {
        // 1. 非空校验
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 2. 大小校验
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像文件大小不能超过 2MB");
        }

        // 3. 扩展名白名单校验（防止上传非图片文件）
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png/gif/webp 格式的图片");
        }

        // 4. 生成随机文件名（UUID 无扩展名污染，避免路径穿越）
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 5. 创建目录并落盘
        String uploadRoot = System.getProperty("user.dir") + File.separator + "uploads";
        File avatarDir = new File(uploadRoot, AVATAR_SUB_DIR);
        if (!avatarDir.exists() && !avatarDir.mkdirs()) {
            throw new RuntimeException("创建头像上传目录失败");
        }
        File dest = new File(avatarDir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("头像保存失败：" + e.getMessage());
        }

        // 6. 返回浏览器可访问的 URL
        return "/uploads/" + AVATAR_SUB_DIR + "/" + filename;
    }
}
