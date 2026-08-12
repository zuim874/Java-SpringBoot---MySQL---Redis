package com.xuwenye.demo.Controller.Product;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.ProductImage;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Service.FileStorageService;
import com.xuwenye.demo.Service.ProductService;
import com.xuwenye.demo.Service.SellerService;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
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
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public ProductController(ProductService productService,
                              SellerService sellerService,
                              FileStorageService fileStorageService,
                              UserService userService,
                              JwtUtil jwtUtil) {
        this.productService = productService;
        this.sellerService = sellerService;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
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
     * 1.校验管理员权限
     * 2.保存商品（含分布式锁保护的缓存更新）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param product 商品实体
     * @return Result 200 新增成功
     */
    @PostMapping("/admin/add")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> addProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody Product product
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = productService.saveProduct(product);
        if (success) {
            return Result.ok("商品新增成功");
        }
        return Result.error(400, "商品新增失败");
    }

    /**
     * 更新商品信息
     * 1.校验管理员权限
     * 2.更新商品信息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 商品ID
     * @param product 商品实体（更新字段）
     * @return Result 200 更新成功
     */
    @PutMapping("/admin/update/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> updateProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id,
            @RequestBody Product product
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        product.setId(id);
        boolean success = productService.updateProduct(product);
        if (success) {
            return Result.ok("商品更新成功");
        }
        return Result.error(400, "商品更新失败");
    }

    /**
     * 逻辑删除商品
     * 1.校验管理员权限
     * 2.逻辑删除商品
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 商品ID
     * @return Result 200 删除成功
     */
    @DeleteMapping("/admin/delete/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> deleteProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = productService.deleteProduct(id);
        if (success) {
            return Result.ok("商品删除成功");
        }
        return Result.error(400, "商品删除失败");
    }

    /**
     * 上架商品
     * 1.校验管理员权限
     * 2.上架商品（status=1）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 商品ID
     * @return Result 200 上架成功
     */
    @PutMapping("/admin/onshelf/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> onShelfProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = productService.onShelfProduct(id);
        if (success) {
            return Result.ok("商品上架成功");
        }
        return Result.error(400, "商品上架失败");
    }

    /**
     * 下架商品
     * 1.校验管理员权限
     * 2.下架商品（status=0）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 商品ID
     * @return Result 200 下架成功
     */
    @PutMapping("/admin/offshelf/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> offShelfProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = productService.offShelfProduct(id);
        if (success) {
            return Result.ok("商品下架成功");
        }
        return Result.error(400, "商品下架失败");
    }

    /**
     * 扣减商品库存（分布式锁保护）
     * 1.校验管理员权限
     * 2.使用分布式锁保护库存扣减操作
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 商品ID
     * @param quantity 扣减数量
     * @return Result 200 扣减成功
     */
    @PutMapping("/admin/deduct-stock/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "库存操作过于频繁，请稍后再试")
    public Result<?> deductStock(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(1) int quantity
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = productService.deductStock(id, quantity);
        if (success) {
            return Result.ok("库存扣减成功");
        }
        return Result.error(400, "库存扣减失败，可能库存不足");
    }

    /**
     * 上传商品图片
     * 1.校验管理员权限
     * 2.上传图片到服务器
     * 3.保存图片记录到数据库
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param productId 商品ID
     * @param file 上传的图片文件
     * @param isMain 是否为主图（0否 1是）
     * @param sort 排序号
     * @return Result 200 上传成功
     */
    @PostMapping("/admin/upload-image")
    @RateLimit(window = 60, maxRequests = 5, message = "图片上传过于频繁，请稍后再试")
    public Result<?> uploadProductImage(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int isMain,
            @RequestParam(defaultValue = "0") int sort
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
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
        if (success) {
            return Result.ok(imageUrl);
        }
        return Result.error(400, "图片保存失败");
    }

    /**
     * 删除商品图片
     * 1.校验管理员权限
     * 2.删除图片文件和数据库记录
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param imageId 图片ID
     * @param productId 商品ID
     * @return Result 200 删除成功
     */
    @DeleteMapping("/admin/delete-image")
    @RateLimit(window = 60, maxRequests = 5, message = "图片操作过于频繁，请稍后再试")
    public Result<?> deleteProductImage(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long imageId,
            @RequestParam @Min(1) Long productId
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
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
            // 删除文件
            fileStorageService.deleteProductImage(targetImage.getImageUrl());
            return Result.ok("图片删除成功");
        }
        return Result.error(400, "图片删除失败");
    }

    /**
     * 新增卖家
     * 1.校验管理员权限
     * 2.保存卖家信息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param seller 卖家实体
     * @return Result 200 新增成功
     */
    @PostMapping("/admin/seller/add")
    @RateLimit(window = 60, maxRequests = 5, message = "卖家操作过于频繁，请稍后再试")
    public Result<?> addSeller(
            @RequestHeader("Authorization") String token,
            @RequestBody Seller seller
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = sellerService.saveSeller(seller);
        if (success) {
            return Result.ok("卖家新增成功");
        }
        return Result.error(400, "卖家新增失败");
    }

    /**
     * 更新卖家信息
     * 1.校验管理员权限
     * 2.更新卖家信息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 卖家ID
     * @param seller 卖家实体（更新字段）
     * @return Result 200 更新成功
     */
    @PutMapping("/admin/seller/update/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "卖家操作过于频繁，请稍后再试")
    public Result<?> updateSeller(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id,
            @RequestBody Seller seller
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        seller.setId(id);
        boolean success = sellerService.updateSeller(seller);
        if (success) {
            return Result.ok("卖家更新成功");
        }
        return Result.error(400, "卖家更新失败");
    }

    /**
     * 删除卖家
     * 1.校验管理员权限
     * 2.逻辑删除卖家
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 卖家ID
     * @return Result 200 删除成功
     */
    @DeleteMapping("/admin/seller/delete/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "卖家操作过于频繁，请稍后再试")
    public Result<?> deleteSeller(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id
    ) {
        // 权限校验
        String username = validateAdmin(token);
        if (username == null) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = sellerService.deleteSeller(id);
        if (success) {
            return Result.ok("卖家删除成功");
        }
        return Result.error(400, "卖家删除失败");
    }

    // ======================== 商家管理接口（需 SELLER 角色） ========================

    /**
     * 商家查看自己的商品列表（分页）
     * 1.校验商家身份（userRole 包含 SELLER）
     * 2.返回该卖家的商品列表（分页）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @return Result 分页商品列表
     */
    @GetMapping("/seller/products")
    @RateLimit(window = 60, maxRequests = 20, message = "商品列表请求过于频繁，请稍后再试")
    public Result<?> getSellerProducts(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 校验商家身份
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }

        Page<Product> productPage = productService.getSellerProductsPage(page, size, sellerId);
        return Result.ok(productPage);
    }

    /**
     * 商家新增商品
     * 1.校验商家身份
     * 2.设置 sellerId 为当前商家
     * 3.保存商品
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param product 商品实体
     * @return Result 200 新增成功
     */
    @PostMapping("/seller/add")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> addSellerProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody Product product) {
        // 校验商家身份
        Long sellerId = validateSeller(token);
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
            return Result.ok("商品新增成功");
        }
        return Result.error(400, "商品新增失败");
    }

    /**
     * 商家更新商品
     * 1.校验商家身份
     * 2.校验商品归属
     * 3.更新商品信息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 商品ID
     * @param product 商品更新信息
     * @return Result 200 更新成功
     */
    @PutMapping("/seller/update/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> updateSellerProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id,
            @RequestBody Product product) {
        // 校验商家身份
        Long sellerId = validateSeller(token);
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
     * 商家上架商品
     * 1.校验商家身份
     * 2.校验商品归属
     * 3.上架商品
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 商品ID
     * @return Result 200 上架成功
     */
    @PutMapping("/seller/onshelf/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> onShelfSellerProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        // 校验商家身份
        Long sellerId = validateSeller(token);
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
     * 1.校验商家身份
     * 2.校验商品归属
     * 3.下架商品
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 商品ID
     * @return Result 200 下架成功
     */
    @PutMapping("/seller/offshelf/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "商品操作过于频繁，请稍后再试")
    public Result<?> offShelfSellerProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        // 校验商家身份
        Long sellerId = validateSeller(token);
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

    // ======================== 内部工具方法 ========================

    /**
     * 校验管理员权限
     * 1.校验 token 有效性
     * 2.校验用户角色为 ROLE_ADMIN
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @return String 用户名（null表示校验失败）
     */
    private String validateAdmin(String token) {
        // 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return null;
        }
        // 解析用户名
        String username = jwtUtil.parseUsername(realToken);
        // 权限校验：仅管理员可操作
        if (!userService.isAdmin(username)) {
            return null;
        }
        return username;
    }

    /**
     * 校验商家身份
     * 1.校验 token 有效性
     * 2.校验用户角色包含 SELLER
     * 3.返回 sellerId（用于后续商品归属校验）
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @return Long sellerId（null表示校验失败）
     */
    private Long validateSeller(String token) {
        // 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return null;
        }
        // 解析用户名
        String username = jwtUtil.parseUsername(realToken);
        // 查询用户信息
        com.xuwenye.demo.Entity.User user = userService.findAllUser(username);
        if (user == null) {
            return null;
        }
        // 校验角色是否包含 SELLER
        String userRole = user.getUserRole();
        if (userRole == null || !userRole.contains("SELLER")) {
            return null;
        }
        // 获取该用户关联的卖家ID
        // 此处通过 SellerMapper 查询用户对应的卖家
        // 由于用户和卖家目前没有直接关联字段，使用用户的用户名作为卖家名称查询
        // 实际项目中可以通过 user_seller 关联表实现
        com.xuwenye.demo.Entity.Seller seller = sellerService.getSellerBySellerName(username);
        if (seller == null) {
            // 如果找不到卖家，返回 0 表示无法操作
            return 0L;
        }
        return seller.getId();
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
        item.put("mainImageUrl", product.getMainImageUrl());
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
