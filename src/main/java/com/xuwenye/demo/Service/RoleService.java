package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuwenye.demo.Entity.Role;
import com.xuwenye.demo.Mapper.RoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色管理业务层
 * 1.根据角色编码查询角色
 * 2.查询所有启用状态的角色
 * <p>
 * @author ZuiM
 */
@Service
public class RoleService {
    private final RoleMapper roleMapper;
    public RoleService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * 根据角色编码查询角色
     * 1.构造查询条件（role_code）
     * 2.查询单条记录
     * <p>
     * @author ZuiM
     * @param roleCode 角色编码
     * @return Role 角色（可能为 null）
     */
    public Role findByCode(String roleCode) {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.eq("role_code", roleCode);
        return roleMapper.selectOne(wrapper);
    }

    /**
     * 查询所有启用角色
     * 1.构造查询条件（status=1）
     * 2.查询列表
     * <p>
     * @author ZuiM
     * @return List&lt;Role&gt; 启用角色列表
     */
    public List<Role> findAllActive() {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return roleMapper.selectList(wrapper);
    }
}
