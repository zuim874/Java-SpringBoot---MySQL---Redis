package com.xuweney.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    //根据用户名查询用户
    public User findUsername(String username) {
        //QueryWrapper<实体> wrapper = new QueryWrapper<>();创造一个条件构造器
//        QueryWrapper<User> wrapper = new QueryWrapper<>();
        //wrapper.eq("数据表字段名",传入参数);用条件构造器判断值是否相等
//        wrapper.eq("username",username);
        //导入mapper层（user）查询方法，selectone匹配一个符合条件的值，结合上一行使匹配username
        return userMapper.findByUsernameAll(username);  // 用自定义方法
    }

    public User findUsernameforlogin(String username) {
        //QueryWrapper<实体> wrapper = new QueryWrapper<>();创造一个条件构造器
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        //wrapper.eq("数据表字段名",传入参数);用条件构造器判断值是否相等
        wrapper.eq("username",username);
        //检验是否删除,筛选出未删除的（使用Mybatis的自动过滤，此处可省略）
//        wrapper.eq("is_deleted",0);
        //导入mapper层（user）查询方法，selectone匹配一个符合条件的值，结合上一行使匹配username
        return userMapper.selectOne(wrapper);
    }

    //根据昵称查询用户
    public List<User> findNickname(String nickname) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("nickname",nickname);
        return userMapper.selectList(wrapper);
    }

    //根据用户状态查询用户
    public List<User> findStatus(String status) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status",status);
        return userMapper.selectList(wrapper);
    }

    //保存用户
    public boolean save(User user) {
        return userMapper.insert(user) > 0;
    }

    //删除用户(逻辑删除)
    public boolean deleteById(Long id) { return userMapper.deleteById(id) > 0;
    }
}