package com.xuwenye.demo.service;

import com.xuwenye.demo.Service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件存储服务单元测试（纯单元测试，无需 Spring 容器）
 * 1.校验拦截：空文件 / null / 超大小 / 非法扩展名
 * 2.成功保存：生成 /uploads/avatars 与 /uploads/products 路径并落盘（测试后清理）
 * <p>
 * @author ZuiM
 */
class FileStorageServiceTest {

    private final FileStorageService fileStorageService = new FileStorageService();

    // ========== 1. 校验拦截 ==========

    /**
     * null 文件被拒绝
     * <p>
     * @author ZuiM
     */
    @Test
    void null文件被拒绝() {
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeAvatar(null));
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeProductImage(null));
    }

    /**
     * 空文件（0 字节）被拒绝
     * <p>
     * @author ZuiM
     */
    @Test
    void 空文件被拒绝() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeAvatar(empty));
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeProductImage(empty));
    }

    /**
     * 超过 10MB 大小限制被拒绝
     * <p>
     * @author ZuiM
     */
    @Test
    void 超大小被拒绝() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeAvatar(file));
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeProductImage(file));
    }

    /**
     * 非法扩展名（可执行文件 / 无扩展名）被拒绝
     * <p>
     * @author ZuiM
     */
    @Test
    void 非法扩展名被拒绝() {
        MockMultipartFile exe = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[100]);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeAvatar(exe));
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeProductImage(exe));

        MockMultipartFile noExt = new MockMultipartFile("file", "noext", "image/png", new byte[100]);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeAvatar(noExt));
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeProductImage(noExt));
    }

    // ========== 2. 成功保存 ==========

    /**
     * 合法头像保存成功，返回正确 URL，落盘文件可删除
     * <p>
     * @author ZuiM
     */
    @Test
    void 合法头像保存成功() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[1024]);
        String url = fileStorageService.storeAvatar(file);
        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/avatars/"));
        assertTrue(url.endsWith(".png"));
        // 清理落盘文件（测试副作用，避免污染项目目录）
        deleteSavedFile(url, "avatars");
    }

    /**
     * 合法商品图片保存成功，返回正确 URL，落盘文件可删除
     * <p>
     * @author ZuiM
     */
    @Test
    void 合法商品图片保存成功() {
        MockMultipartFile file = new MockMultipartFile("file", "p.webp", "image/webp", new byte[2048]);
        String url = fileStorageService.storeProductImage(file);
        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/products/"));
        assertTrue(url.endsWith(".webp"));
        deleteSavedFile(url, "products");
    }

    /**
     * 删除测试期间落盘的图片文件
     * <p>
     * @author ZuiM
     * @param url 返回的图片 URL（如 /uploads/avatars/xxx.png）
     * @param subDir 子目录（avatars / products）
     */
    private void deleteSavedFile(String url, String subDir) {
        String uploadRoot = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + subDir;
        File saved = new File(uploadRoot, url.substring(url.lastIndexOf('/') + 1));
        if (saved.exists()) {
            saved.delete();
        }
    }
}
