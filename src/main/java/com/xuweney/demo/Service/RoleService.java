package com.xuweney.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuweney.demo.Entity.Role;
import com.xuweney.demo.Mapper.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色管理业务层
 */
@Service
public class RoleService {
    private final RoleMapper roleMapper;
    public RoleService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * 根据角色编码查询
     */
    public Role findByCode(String roleCode) {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.eq("role_code", roleCode);
        return roleMapper.selectOne(wrapper);
    }

    /**
     * 查询所有可用角色
     */
    public List<Role> findAllActive() {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return roleMapper.selectList(wrapper);
    }
}