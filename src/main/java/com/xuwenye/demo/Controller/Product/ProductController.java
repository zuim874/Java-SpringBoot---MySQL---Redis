package com.xuwenye.demo.Controller.Product;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.ProductImage;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.FileStorageService;
import com.xuwenye.demo.Service.ProductService;
import com.xuwenye.demo.Service.SellerService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.annotation.UserCheck;
import com.xuwenye.demo.common.Result;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品管理接口
 * 1.公开接口：商品列表、商品详情、分类列表（IP+用户限流，30次/60s）
 * 2.管理接口：商品CRUD、上下架、图片管理（分布式锁+限流，5次/60s）
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/product")
@Validated
public class ProductController {

    private final ProductService productService;
    private final SellerService sellerService;
    private final FileStorageService fileStorageService;

    public ProductController(ProductService productService,
                              SellerService sellerService,
                              FileStorageService fileStorageService) {
        this.productService = productService;
        this.sellerService = sellerService;
        this.fileStorageService = fileStorageService;
    }

    // ======================== 公开接口（无需登录） ========================

    /**
     * 分页查询上架商品（支持按分类筛选）
     * 1.IP限流：30次/60s
     * 2.返回分页商品列表（含卖家信息、主图URL）
     * <p>
     * @author ZuiM
     * @param page 页码（默认1）
     * @param size 每页条数（默认10）
     * @param category 商品分类（可选）
     * @param keyword 搜索关键词（可选，模糊匹配商品名/描述）
     * @return Result 分页商品列表
     */
    @GetMapping("/page")
    @RateLimit(window = 60, maxRequests = 30, message = "商品列表请求过于频繁，请稍后再试")
    public Result<Page<Map<String, Object>>> getProductPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        Page<Product> productPage = productService.getOnShelfProductsPage(page, size, category, keyword);
        Page<Map<String, Object>> resultPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (Product product : productPage.getRecords()) {
            records.add(buildProductResponse(product));
        }
        resultPage.setRecords(records);
        return Result.ok(resultPage);
    }

    /**
     * 获取所有上架商品（保留旧接口兼容）
     * 1.IP限流：30次/60s
     * 2.返回商品列表（含卖家信息、主图URL）
     * <p>
     * @author ZuiM
     * @return Result 商品列表
     */
    @GetMapping("/list")
    @RateLimit(window = 60, maxRequests = 30, message = "商品列表请求过于频繁，请稍后再试")
    public Result<List<Map<String, Object>>> getProductList() {
        List<Product> products = productService.getAllOnShelfProducts();
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (Product product : products) {
            Map<String, Object> item = buildProductResponse(product);
            result.add(item);
        }

        return Result.ok(result);
    }

    /**
     * 按分类获取上架商品
     * 1.IP限流：30次/60s
     * 2.返回指定分类的商品列表
     * <p>
     * @author ZuiM
     * @param category 商品分类
     * @return Result 商品列表
     */
    @GetMapping("/list/category")
    @RateLimit(window = 60, maxRequests = 30, message = "商品列表请求过于频繁，请稍后再试")
    public Result<List<Map<String, Object>>> getProductsByCategory(@RequestParam String category) {
        List<Product> products = productService.getProductsByCategory(category);
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (Product product : products) {
            Map<String, Object> item = buildProductResponse(product);
            result.add(item);
        }

        return Result.ok(result);
    }

    /**
     * 获取商品详情
     * 1.IP限流：30次/60s
     * 2.返回商品详情（含卖家信息、所有图片）
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return Result 商品详情
     */
    @GetMapping("/detail/{id}")
    @RateLimit(window = 60, maxRequests = 30, message = "商品详情请求过于频繁，请稍后再试")
    public Result<?> getProductDetail(@PathVariable @Min(1) Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Result.error(400, "商品不存在或已下架");
        }

        Map<String, Object> result = buildProductResponse(product);

        // 添加所有图片
        List<ProductImage> images = productService.getProductImages(id);
        result.put("images", images);

        return Result.ok(result);
    }

    /**
     * 获取所有分类列表
     * 1.IP限流：30次/60s
     * 2.返回分类列表
     * <p>
     * @author ZuiM
     * @return Result 分类列表
     */
    @GetMapping("/categories")
    @RateLimit(window = 60, maxRequests = 30, message = "分类请求过于频繁，请稍后再试")
    public Result<List<String>> getAllCategories() {
        List<String> categories = productService.getAllCategories();
        return Result.ok(categories);
    }

    /**
     * 获取所有卖家列表
     * 1.IP限流：30次/60s
     * 2.返回卖家列表
     * <p>
     * @author ZuiM
     * @return Result 卖家列表
     */
    @GetMapping("/sellers")
    @RateLimit(window = 60, maxRequests = 30, message = "卖家列表请求过于频繁，请稍后再试")
    public Result<List<Seller>> getAllSellers() {
        List<Seller> sellers = sellerService.getAllSellers();
        return Result.ok(sellers);
    }

    /**
     * 获取卖家详情
     * 1.IP限流：30次/60s
     * 2.返回卖家详情
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     * @return Result 卖家详情
     */
    @GetMapping("/seller/{id}")
    @RateLimit(window = 60, maxRequests = 30, message = "卖家详情请求过于频繁，请稍后再试")
    public Result<?> getSellerDetail(@PathVariable @Min(1) Long id) {
        Seller seller = sellerService.getSellerById(id);
        if (seller == null) {
            return Result.error(400, "卖家不存在");
        }
        return Result.ok(seller);
    }

    // ======================== 管理接口（需管理员登录） ========================

    /**
     * 新增商品
     * 1.@UserCheck 切面校验管理员权限
     * 2.保存商品（含分布式锁保护的缓存更新）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param product 商品实体
     * @return Result 200 新增成功
     */
    @OperationLog("新增商品")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PostMapping("/admin/add")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> addProduct(
            User currentUser,
            @RequestBody Product product
    ) {
        boolean success = productService.saveProduct(product);
        if (success) {
            return Result.ok("商品新增成功");
        }
        return Result.error(400, "商品新增失败");
    }

    /**
     * 更新商品信息
     * 1.@UserCheck 切面校验管理员权限
     * 2.更新商品信息
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @param product 商品实体（更新字段）
     * @return Result 200 更新成功
     */
    @OperationLog("更新商品")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PutMapping("/admin/update/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> updateProduct(
            User currentUser,
            @PathVariable @Min(1) Long id,
            @RequestBody Product product
    ) {
        product.setId(id);
        boolean success = productService.updateProduct(product);
        if (success) {
            return Result.ok("商品更新成功");
        }
        return Result.error(400, "商品更新失败");
    }

    /**
     * 逻辑删除商品
     * 1.@UserCheck 切面校验管理员权限
     * 2.逻辑删除商品
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @return Result 200 删除成功
     */
    @OperationLog("删除商品")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @DeleteMapping("/admin/delete/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> deleteProduct(
            User currentUser,
            @PathVariable @Min(1) Long id
    ) {
        boolean success = productService.deleteProduct(id);
        if (success) {
            return Result.ok("商品删除成功");
        }
        return Result.error(400, "商品删除失败");
    }

    /**
     * 上架商品
     * 1.@UserCheck 切面校验管理员权限
     * 2.上架商品（status=1）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @return Result 200 上架成功
     */
    @OperationLog("商品上架")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PutMapping("/admin/onshelf/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> onShelfProduct(
            User currentUser,
            @PathVariable @Min(1) Long id
    ) {
        boolean success = productService.onShelfProduct(id);
        if (success) {
            return Result.ok("商品上架成功");
        }
        return Result.error(400, "商品上架失败");
    }

    /**
     * 下架商品
     * 1.@UserCheck 切面校验管理员权限
     * 2.下架商品（status=0）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @return Result 200 下架成功
     */
    @OperationLog("商品下架")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PutMapping("/admin/offshelf/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> offShelfProduct(
            User currentUser,
            @PathVariable @Min(1) Long id
    ) {
        boolean success = productService.offShelfProduct(id);
        if (success) {
            return Result.ok("商品下架成功");
        }
        return Result.error(400, "商品下架失败");
    }

    /**
     * 扣减商品库存（分布式锁保护）
     * 1.@UserCheck 切面校验管理员权限
     * 2.使用分布式锁保护库存扣减操作
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @param quantity 扣减数量
     * @return Result 200 扣减成功
     */
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PutMapping("/admin/deduct-stock/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "库存操作过于频繁，请稍后再试")
    public Result<?> deductStock(
            User currentUser,
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(1) int quantity
    ) {
        boolean success = productService.deductStock(id, quantity);
        if (success) {
            return Result.ok("库存扣减成功");
        }
        return Result.error(400, "库存扣减失败，可能库存不足");
    }

    /**
     * 上传商品图片
     * 1.@UserCheck 切面校验管理员权限
     * 2.上传图片到服务器
     * 3.保存图片记录到数据库
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param productId 商品ID
     * @param file 上传的图片文件
     * @param isMain 是否为主图（0否 1是）
     * @param sort 排序号
     * @return Result 200 上传成功
     */
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PostMapping("/admin/upload-image")
    @RateLimit(window = 60, maxRequests = 5, message = "图片上传过于频繁，请稍后再试")
    public Result<?> uploadProductImage(
            User currentUser,
            @RequestParam @Min(1) Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int isMain,
            @RequestParam(defaultValue = "0") int sort
    ) {
        // 保存图片文件
        String imageUrl = fileStorageService.storeProductImage(file);

        // 保存图片记录
        ProductImage productImage = new ProductImage();
        productImage.setProductId(productId);
        productImage.setImageUrl(imageUrl);
        productImage.setIsMain(isMain);
        productImage.setSort(sort);

        boolean success = productService.addProductImage(productImage);
        if (success) {
            return Result.ok(imageUrl);
        }
        return Result.error(400, "图片保存失败");
    }

    /**
     * 删除商品图片
     * 1.@UserCheck 切面校验管理员权限
     * 2.删除图片文件和数据库记录
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param imageId 图片ID
     * @param productId 商品ID
     * @return Result 200 删除成功
     */
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @DeleteMapping("/admin/delete-image")
    @RateLimit(window = 60, maxRequests = 5, message = "图片操作过于频繁，请稍后再试")
    public Result<?> deleteProductImage(
            User currentUser,
            @RequestParam @Min(1) Long imageId,
            @RequestParam @Min(1) Long productId
    ) {
        // 获取图片信息
        List<ProductImage> images = productService.getProductImages(productId);
        ProductImage targetImage = null;
        for (ProductImage img : images) {
            if (img.getId().equals(imageId)) {
                targetImage = img;
                break;
            }
        }

        if (targetImage == null) {
            return Result.error(400, "图片不存在");
        }

        // 删除数据库记录
        boolean success = productService.deleteProductImage(imageId, productId);
        if (success) {
            // 删除文件
            fileStorageService.deleteProductImage(targetImage.getImageUrl());
            return Result.ok("图片删除成功");
        }
        return Result.error(400, "图片删除失败");
    }

    /**
     * 新增卖家
     * 1.@UserCheck 切面校验管理员权限
     * 2.保存卖家信息
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param seller 卖家实体
     * @return Result 200 新增成功
     */
    @OperationLog("新增卖家")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PostMapping("/admin/seller/add")
    @RateLimit(window = 60, maxRequests = 5, message = "卖家操作过于频繁，请稍后再试")
    public Result<?> addSeller(
            User currentUser,
            @RequestBody Seller seller
    ) {
        boolean success = sellerService.saveSeller(seller);
        if (success) {
            return Result.ok("卖家新增成功");
        }
        return Result.error(400, "卖家新增失败");
    }

    /**
     * 更新卖家信息
     * 1.@UserCheck 切面校验管理员权限
     * 2.更新卖家信息
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 卖家ID
     * @param seller 卖家实体（更新字段）
     * @return Result 200 更新成功
     */
    @OperationLog("更新卖家")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PutMapping("/admin/seller/update/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "卖家操作过于频繁，请稍后再试")
    public Result<?> updateSeller(
            User currentUser,
            @PathVariable @Min(1) Long id,
            @RequestBody Seller seller
    ) {
        seller.setId(id);
        boolean success = sellerService.updateSeller(seller);
        if (success) {
            return Result.ok("卖家更新成功");
        }
        return Result.error(400, "卖家更新失败");
    }

    /**
     * 删除卖家
     * 1.@UserCheck 切面校验管理员权限
     * 2.逻辑删除卖家
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 卖家ID
     * @return Result 200 删除成功
     */
    @OperationLog("删除卖家")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @DeleteMapping("/admin/seller/delete/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "卖家操作过于频繁，请稍后再试")
    public Result<?> deleteSeller(
            User currentUser,
            @PathVariable @Min(1) Long id
    ) {
        boolean success = sellerService.deleteSeller(id);
        if (success) {
            return Result.ok("卖家删除成功");
        }
        return Result.error(400, "卖家删除失败");
    }

    // ======================== 商家管理接口（需 SELLER 角色） ========================

    /**
     * 商家查看自己的商品列表（分页）
     * 1.@UserCheck 切面校验商家身份
     * 2.返回该卖家的商品列表（分页）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param page 页码
     * @param size 每页条数
     * @return Result 分页商品列表
     */
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @GetMapping("/seller/products")
    @RateLimit(window = 60, maxRequests = 20, message = "商品列表请求过于频繁，请稍后再试")
    public Result<?> getSellerProducts(
            User currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 解析当前商家的卖家ID
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }

        Page<Product> productPage = productService.getSellerProductsPage(page, size, sellerId);
        return Result.ok(productPage);
    }

    /**
     * 商家新增商品
     * 1.@UserCheck 切面校验商家身份
     * 2.设置 sellerId 为当前商家
     * 3.保存商品
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param product 商品实体
     * @return Result 200 返回新商品（含自增ID，供后续上传主图/细节图使用）
     */
    @OperationLog("商家新增商品")
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @PostMapping("/seller/add")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> addSellerProduct(
            User currentUser,
            @RequestBody Product product) {
        // 解析当前商家的卖家ID
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }

        // 设置卖家ID
        product.setSellerId(sellerId);
        // 新商品默认为下架状态
        if (product.getStatus() == null) {
            product.setStatus(0);
        }

        boolean success = productService.saveProduct(product);
        if (success) {
            return Result.ok(product);
        }
        return Result.error(400, "商品新增失败");
    }

    /**
     * 商家更新商品
     * 1.@UserCheck 切面校验商家身份
     * 2.校验商品归属
     * 3.更新商品信息
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @param product 商品更新信息
     * @return Result 200 更新成功
     */
    @OperationLog("商家更新商品")
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @PutMapping("/seller/update/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> updateSellerProduct(
            User currentUser,
            @PathVariable @Min(1) Long id,
            @RequestBody Product product) {
        // 解析当前商家的卖家ID
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }

        // 校验商品归属
        Product existing = productService.getProductById(id);
        if (existing == null) {
            return Result.error(400, "商品不存在");
        }
        if (!existing.getSellerId().equals(sellerId)) {
            return Result.error(403, "无权操作其他商家的商品");
        }

        product.setId(id);
        product.setSellerId(null); // 不允许修改卖家ID
        boolean success = productService.updateProduct(product);
        if (success) {
            return Result.ok("商品更新成功");
        }
        return Result.error(400, "商品更新失败");
    }

    /**
     * 商家上传商品图片（主图/细节图）
     * 1.@UserCheck 切面校验商家身份
     * 2.校验商品归属
     * 3.保存图片记录；主图同步更新 sys_product.main_image_url 冗余字段
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param productId 商品ID
     * @param file 上传的图片文件
     * @param isMain 是否主图（0细节图 1主图）
     * @param sort 排序号（细节图排序）
     * @return Result 200 返回图片访问URL
     */
    @OperationLog("商家上传商品图片")
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @PostMapping("/seller/upload-image")
    @RateLimit(window = 60, maxRequests = 10, message = "图片上传过于频繁，请稍后再试")
    public Result<?> uploadSellerProductImage(
            User currentUser,
            @RequestParam @Min(1) Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int isMain,
            @RequestParam(defaultValue = "0") int sort
    ) {
        // 校验商品归属
        Product existing = productService.getProductById(productId);
        if (existing == null) {
            return Result.error(400, "商品不存在");
        }
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null || !existing.getSellerId().equals(sellerId)) {
            return Result.error(403, "无权操作其他商家的商品");
        }

        // 保存图片文件
        String imageUrl = fileStorageService.storeProductImage(file);

        // 保存图片记录
        ProductImage productImage = new ProductImage();
        productImage.setProductId(productId);
        productImage.setImageUrl(imageUrl);
        productImage.setIsMain(isMain);
        productImage.setSort(sort);

        boolean success = productService.addProductImage(productImage);
        if (!success) {
            return Result.error(400, "图片保存失败");
        }

        // 主图同步更新商品冗余字段（并触发详情缓存刷新）
        if (isMain == 1) {
            Product update = new Product();
            update.setId(productId);
            update.setMainImageUrl(imageUrl);
            productService.updateProduct(update);
        }

        return Result.ok(imageUrl);
    }

    /**
     * 商家删除商品图片
     * 1.@UserCheck 切面校验商家身份
     * 2.校验商品归属
     * 3.删除图片记录和物理文件
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param imageId 图片ID
     * @param productId 商品ID
     * @return Result 200 删除成功
     */
    @OperationLog("商家删除商品图片")
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @DeleteMapping("/seller/delete-image")
    @RateLimit(window = 60, maxRequests = 10, message = "图片操作过于频繁，请稍后再试")
    public Result<?> deleteSellerProductImage(
            User currentUser,
            @RequestParam @Min(1) Long imageId,
            @RequestParam @Min(1) Long productId
    ) {
        // 校验商品归属
        Product existing = productService.getProductById(productId);
        if (existing == null) {
            return Result.error(400, "商品不存在");
        }
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null || !existing.getSellerId().equals(sellerId)) {
            return Result.error(403, "无权操作其他商家的商品");
        }

        // 获取图片信息
        List<ProductImage> images = productService.getProductImages(productId);
        ProductImage targetImage = null;
        for (ProductImage img : images) {
            if (img.getId().equals(imageId)) {
                targetImage = img;
                break;
            }
        }
        if (targetImage == null) {
            return Result.error(400, "图片不存在");
        }

        // 删除数据库记录
        boolean success = productService.deleteProductImage(imageId, productId);
        if (success) {
            // 删除物理文件
            fileStorageService.deleteProductImage(targetImage.getImageUrl());
            return Result.ok("图片删除成功");
        }
        return Result.error(400, "图片删除失败");
    }

    /**
     * 商家上架商品
     * 1.@UserCheck 切面校验商家身份
     * 2.校验商品归属
     * 3.上架商品
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @return Result 200 上架成功
     */
    @OperationLog("商家上架商品")
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @PutMapping("/seller/onshelf/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> onShelfSellerProduct(
            User currentUser,
            @PathVariable @Min(1) Long id) {
        // 解析当前商家的卖家ID
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }

        // 校验商品归属
        Product existing = productService.getProductById(id);
        if (existing == null) {
            return Result.error(400, "商品不存在");
        }
        if (!existing.getSellerId().equals(sellerId)) {
            return Result.error(403, "无权操作其他商家的商品");
        }

        boolean success = productService.onShelfProduct(id);
        if (success) {
            return Result.ok("商品上架成功");
        }
        return Result.error(400, "商品上架失败");
    }

    /**
     * 商家下架商品
     * 1.@UserCheck 切面校验商家身份
     * 2.校验商品归属
     * 3.下架商品
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @return Result 200 下架成功
     */
    @OperationLog("商家下架商品")
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @PutMapping("/seller/offshelf/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> offShelfSellerProduct(
            User currentUser,
            @PathVariable @Min(1) Long id) {
        // 解析当前商家的卖家ID
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }

        // 校验商品归属
        Product existing = productService.getProductById(id);
        if (existing == null) {
            return Result.error(400, "商品不存在");
        }
        if (!existing.getSellerId().equals(sellerId)) {
            return Result.error(403, "无权操作其他商家的商品");
        }

        boolean success = productService.offShelfProduct(id);
        if (success) {
            return Result.ok("商品下架成功");
        }
        return Result.error(400, "商品下架失败");
    }

    /**
     * 商家设置商品为推荐/取消推荐（会员卖家权益，商城置顶曝光）
     * 1.@UserCheck 切面校验商家身份
     * 2.校验商品归属
     * 3.校验是否为会员卖家（ROLE_VIP_SELLER）
     * 4.设置推荐位
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 商品ID
     * @param recommend 1推荐 0取消
     * @return Result 200 操作成功
     */
    @OperationLog("商家设置商品推荐位")
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @PutMapping("/seller/recommend/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> setSellerRecommend(
            User currentUser,
            @PathVariable @Min(1) Long id,
            @RequestParam int recommend) {
        // 解析当前商家的卖家ID
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        // 校验商品归属
        Product existing = productService.getProductById(id);
        if (existing == null) {
            return Result.error(400, "商品不存在");
        }
        if (!existing.getSellerId().equals(sellerId)) {
            return Result.error(403, "无权操作其他商家的商品");
        }
        // 校验会员卖家身份（ROLE_VIP_SELLER）
        if (currentUser.getUserRole() == null || !currentUser.getUserRole().contains("VIP_SELLER")) {
            return Result.error(403, "推荐位为会员卖家专属权益，请先升级为会员卖家");
        }
        boolean success = productService.setRecommend(id, recommend);
        if (success) {
            return Result.ok(recommend == 1 ? "商品已置顶推荐" : "已取消推荐");
        }
        return Result.error(400, "操作失败");
    }

    // ======================== 内部工具方法 ========================

    /**
     * 根据登录用户ID解析卖家ID（卖家登录后经 user_id 直查店铺，替代「用户名 = 卖家名称」约定）
     * 登录态与 SELLER 角色校验已由 @UserCheck 切面完成，此处仅做卖家归属解析
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @return Long 卖家ID（null=无对应卖家）
     */
    private Long resolveSellerId(User currentUser) {
        if (currentUser == null) {
            return null;
        }
        Seller seller = sellerService.getSellerByUserId(currentUser.getId());
        return seller == null ? null : seller.getId();
    }

    /**
     * 构建商品响应数据（包含卖家信息）
     * <p>
     * @author ZuiM
     * @param product 商品实体
     * @return Map<String, Object> 商品响应数据
     */
    private Map<String, Object> buildProductResponse(Product product) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", product.getId());
        item.put("sellerId", product.getSellerId());
        item.put("productName", product.getProductName());
        item.put("price", product.getPrice());
        item.put("stock", product.getStock());
        item.put("sold", product.getSold());
        item.put("status", product.getStatus());
        item.put("description", product.getDescription());
        item.put("category", product.getCategory());
        // 分类ID集合翻译为分类名称列表（商品表 category 存分类ID，展示需用名称）
        item.put("categoryNames", productService.getCategoryNames(product.getCategory()));
        item.put("mainImageUrl", product.getMainImageUrl());
        item.put("recommend", product.getRecommend());
        item.put("createTime", product.getCreateTime());
        item.put("updateTime", product.getUpdateTime());

        // 添加卖家信息
        Seller seller = productService.getSellerById(product.getSellerId());
        if (seller != null) {
            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("id", seller.getId());
            sellerInfo.put("sellerName", seller.getSellerName());
            sellerInfo.put("address", seller.getAddress());
            sellerInfo.put("sellerAvatar", seller.getSellerAvatar());
            sellerInfo.put("sellerContact", seller.getSellerContact());
            item.put("seller", sellerInfo);
        }

        return item;
    }
}
