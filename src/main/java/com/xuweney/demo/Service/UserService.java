package com.xuweney.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    // 注入 RedisTemplate，用于缓存用户数据
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //根据用户名查询用户（登录用，含 Redis 缓存）
    public User findUsernameforlogin(String username) {
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:login:" + username;

        // 第 1 步：先从 Redis 查，有则直接返回
        User cached = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        //QueryWrapper<实体> wrapper = new QueryWrapper<>();创造一个条件构造器
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        //wrapper.eq("数据表字段名",传入参数);用条件构造器判断值是否相等
        wrapper.eq("username",username);
        //检验是否删除,筛选出未删除的（使用Mybatis的自动过滤，此处可省略）
//        wrapper.eq("is_deleted",0);
        //导入mapper层（user）查询方法，selectone匹配一个符合条件的值，结合上一行使匹配username
        User user = userMapper.selectOne(wrapper);

        // 第 3 步：查到了就写入 Redis（10 分钟过期，避免数据长期不一致）
        if (user != null) {
            redisTemplate.opsForValue().set(cacheKey, user, 10, TimeUnit.MINUTES);
        }

        return user;
    }

    //根据用户名查询用户（包含已逻辑删除的）
    public User findUsername(String username) {
        return userMapper.findByUsernameAll(username);
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

    //根据用户ID查询用户
    public User getById(Long id) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("id",id);
        return userMapper.selectOne(wrapper);
    }

    //保存用户
    public boolean save(User user) {
        return userMapper.insert(user) > 0;
    }

    //删除用户(逻辑删除)
    public boolean deleteById(Long id) { return userMapper.deleteById(id) > 0;
    }

    //恢复用户
    public boolean recoverById(Long id) {
        return userMapper.recoverById(id) > 0;
    }
}