package com.xuwenye.demo.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务：负责头像、商品图片等上传文件的落盘保存与访问路径生成
 * 1.非空校验
 * 2.大小校验（≤10MB）
 * 3.扩展名白名单校验（仅图片格式）
 * 4.服务端生成 UUID 文件名（防路径穿越）
 * 5.落盘保存并返回可访问 URL
 * <p>
 * @author ZuiM
 */
@Service
public class FileStorageService {

    /** 允许的图片扩展名白名单 */
    private static final List<String> ALLOWED_IMAGE_EXT = List.of("jpg", "jpeg", "png", "gif", "webp");

    /** 头像最大体积：10MB */
    private static final long MAX_AVATAR_SIZE = 10 * 1024 * 1024;

    /** 商品图片最大体积：10MB */
    private static final long MAX_PRODUCT_IMAGE_SIZE = 10 * 1024 * 1024;

    /** 头像子目录（相对 upload 根目录） */
    private static final String AVATAR_SUB_DIR = "avatars";

    /** 商品图片子目录（相对 upload 根目录） */
    private static final String PRODUCT_IMAGE_SUB_DIR = "products";

    /**
     * 保存头像文件，返回可访问的 URL 路径
     * 1.非空校验
     * 2.大小校验（10MB 上限）
     * 3.扩展名白名单校验（防止上传非图片文件）
     * 4.生成随机文件名（UUID，避免路径穿越）
     * 5.创建目录并落盘
     * 6.返回浏览器可访问的 URL
     * <p>
     * @author ZuiM
     * @param file 上传的图片文件
     * @return String 例如 /uploads/avatars/xxx.png
     * @throws IllegalArgumentException 文件为空 / 类型不允许 / 超过大小限制
     */
    public String storeAvatar(MultipartFile file) {
        // 1. 非空校验
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 2. 大小校验
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像文件大小不能超过 10MB");
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

    /**
     * 保存商品图片文件，返回可访问的 URL 路径
     * 1.非空校验
     * 2.大小校验（10MB 上限）
     * 3.扩展名白名单校验（防止上传非图片文件）
     * 4.生成随机文件名（UUID，避免路径穿越）
     * 5.创建目录并落盘
     * 6.返回浏览器可访问的 URL
     * <p>
     * @author ZuiM
     * @param file 上传的商品图片文件
     * @return String 例如 /uploads/products/xxx.png
     * @throws IllegalArgumentException 文件为空 / 类型不允许 / 超过大小限制
     */
    public String storeProductImage(MultipartFile file) {
        // 1. 非空校验
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的商品图片不能为空");
        }

        // 2. 大小校验
        if (file.getSize() > MAX_PRODUCT_IMAGE_SIZE) {
            throw new IllegalArgumentException("商品图片大小不能超过 10MB");
        }

        // 3. 扩展名白名单校验（防止上传非图片文件）
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png/gif/webp 格式的商品图片");
        }

        // 4. 生成随机文件名（UUID 无扩展名污染，避免路径穿越）
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 5. 创建目录并落盘
        String uploadRoot = System.getProperty("user.dir") + File.separator + "uploads";
        File productDir = new File(uploadRoot, PRODUCT_IMAGE_SUB_DIR);
        if (!productDir.exists() && !productDir.mkdirs()) {
            throw new RuntimeException("创建商品图片上传目录失败");
        }
        File dest = new File(productDir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("商品图片保存失败：" + e.getMessage());
        }

        // 6. 返回浏览器可访问的 URL
        return "/uploads/" + PRODUCT_IMAGE_SUB_DIR + "/" + filename;
    }

    /**
     * 删除商品图片文件
     * <p>
     * @author ZuiM
     * @param imageUrl 图片URL路径（如 /uploads/products/xxx.png）
     */
    public void deleteProductImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/" + PRODUCT_IMAGE_SUB_DIR)) {
            return; // 只删除本服务管理的商品图片
        }

        // 获取文件相对路径
        String relativePath = imageUrl.substring("/uploads/".length());
        String uploadRoot = System.getProperty("user.dir") + File.separator + "uploads";
        File file = new File(uploadRoot, relativePath);

        if (file.exists() && !file.isDirectory()) {
            file.delete();
        }
    }
}
