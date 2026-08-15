package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.Role;
import com.xuwenye.demo.Mapper.RoleMapper;
import com.xuwenye.demo.Service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 角色业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.按角色编码查询：存在返回 / 不存在返回 null
 * 2.启用角色列表：仅包含 status=1 的角色
 * <p>
 * @author ZuiM
 */
class RoleServiceTest extends AbstractServiceTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleMapper roleMapper;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试角色（编码唯一）
     * <p>
     * @author ZuiM
     * @param roleCode 角色编码
     * @param status 状态 0禁用 1启用
     * @return Role 角色
     */
    private Role buildRole(String roleCode, int status) {
        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName("测试角色" + roleCode);
        role.setStatus(status);
        role.setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role;
    }

    /**
     * 生成唯一的角色编码
     * <p>
     * @author ZuiM
     * @param prefix 前缀
     * @return String 唯一角色编码
     */
    private String uniqueRoleCode(String prefix) {
        return prefix + SUFFIX;
    }

    // ========== 1. 按编码查询 ==========

    /**
     * 按角色编码查询到已存在角色
     * <p>
     * @author ZuiM
     */
    @Test
    void 按编码查询角色() {
        Role role = buildRole(uniqueRoleCode("ROLE_TEST_"), 1);
        Role found = roleService.findByCode(role.getRoleCode());
        assertNotNull(found);
        assertEquals(role.getId(), found.getId());
        roleMapper.deleteById(role.getId());
    }

    /**
     * 不存在的角色编码返回 null
     * <p>
     * @author ZuiM
     */
    @Test
    void 不存在的编码返回空() {
        assertNull(roleService.findByCode(uniqueRoleCode("ROLE_NOT_EXIST_")));
    }

    // ========== 2. 启用角色列表 ==========

    /**
     * 启用角色列表包含启用角色、排除禁用角色
     * <p>
     * @author ZuiM
     */
    @Test
    void 启用角色列表过滤禁用角色() {
        Role enabled = buildRole(uniqueRoleCode("ROLE_EN_"), 1);
        Role disabled = buildRole(uniqueRoleCode("ROLE_DIS_"), 0);

        List<Role> list = roleService.findAllActive();
        assertNotNull(list);
        assertTrue(list.stream().anyMatch(r -> r.getId().equals(enabled.getId())));
        assertFalse(list.stream().anyMatch(r -> r.getId().equals(disabled.getId())));

        roleMapper.deleteById(enabled.getId());
        roleMapper.deleteById(disabled.getId());
    }
}
