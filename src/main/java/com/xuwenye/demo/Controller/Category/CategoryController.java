package com.xuwenye.demo.Controller.Category;

import com.xuwenye.demo.Entity.Category;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.CategoryService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.annotation.UserCheck;
import com.xuwenye.demo.common.Result;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类接口
 * 1.公开接口：启用分类列表（GET，无需登录，供卖家/管理员/买家拉取预设分类）
 * 2.管理接口：新增/删除分类（@UserCheck 切面校验 ROLE_ADMIN 角色）
 * 说明：分类仅允许管理员添加/删除；卖家新增/编辑商品时从分类列表拉取并多选，不可自编辑
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api")
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 获取启用分类列表（公开，无需登录）
     * 返回分类对象数组（含 id/name/sort），前端可用于下拉/多选与 id↔名称 映射
     * <p>
     * @author ZuiM
     * @return Result<List<Category>> 分类列表
     */
    @GetMapping("/category/list")
    @RateLimit(window = 60, maxRequests = 30, message = "分类请求过于频繁，请稍后再试")
    public Result<List<Category>> listCategories() {
        return Result.ok(categoryService.getAllCategories());
    }

    /**
     * 新增分类（仅管理员）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param name 分类名称
     * @param sort 排序号（可选，默认0）
     * @return Result 200 新增成功；400 参数错误/名称重复
     */
    @OperationLog("新增商品分类")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PostMapping("/admin/category/add")
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    public Result<?> addCategory(
            User currentUser,
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int sort) {
        try {
            boolean success = categoryService.addCategory(name, sort);
            if (success) {
                return Result.ok("分类「" + name.trim() + "」新增成功");
            }
            return Result.error(400, "分类新增失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 删除分类（仅管理员）
     * 若该分类已被商品使用，则拒绝删除以保护数据完整性
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 分类ID
     * @return Result 200 删除成功；400 分类不存在/已被商品使用
     */
    @OperationLog("删除商品分类")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @DeleteMapping("/admin/category/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    public Result<?> deleteCategory(
            User currentUser,
            @PathVariable @Min(1) Long id) {
        try {
            boolean success = categoryService.deleteCategory(id);
            if (success) {
                return Result.ok("分类删除成功");
            }
            return Result.error(400, "分类删除失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
