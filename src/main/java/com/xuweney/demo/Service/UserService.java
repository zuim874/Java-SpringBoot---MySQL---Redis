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
        String cacheKey = "demo:user:login:active:" + username;

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
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:login:all:" + username;

        // redis查询
        User cached = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // redis没有，查mysql
        User user = userMapper.findByUsernameAll(username);

        // 写入redis(10分钟过期)
        if (user != null) {
            redisTemplate.opsForValue().set(cacheKey, user, 10, TimeUnit.MINUTES);
        }
        return user;
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

    //检查邮箱是否已存在
    public boolean isEmailExist(String email) {
        return userMapper.findByEmail(email) != null;
    }

    //保存用户
    public boolean save(User user) {
        String username = user.getUsername();
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:login:active:" + username;
        boolean result = userMapper.insert(user) > 0;

        redisTemplate.opsForValue().set(cacheKey, user, 10, TimeUnit.MINUTES);

        return result;
    }

    //删除用户(逻辑删除)
    public boolean deleteById(Long id) {
        // 先查出用户信息，用于删除缓存
        User user = userMapper.selectById(id);
        boolean result = userMapper.deleteById(id) > 0;

        if (result && user != null) {
            // username 唯一，删除该用户对应的缓存即可
            redisTemplate.delete("demo:user:login:active:" + user.getUsername());
            redisTemplate.delete("demo:user:login:all:" + user.getUsername());
        }
        return result;
    }

    //恢复用户
    public boolean recoverById(Long id) {
        boolean result = userMapper.recoverById(id) > 0;
        if (result) {
            User user = userMapper.selectById(id);
            if (user != null) {
                // 恢复后重新写入redis缓存
                String cacheKey = "demo:user:login:active:" + user.getUsername();
                redisTemplate.opsForValue().set(cacheKey, user, 10, TimeUnit.MINUTES);
            }
        }
        return result;
    }

    //检查权限
    public boolean is_admin(String username) {
        // 1. 查询用户
        User user = userMapper.findByUsernameAll(username);

        // 2. 判空
        if (user == null) {
            return false;
        }

        // 3. 获取角色并判空
        String userRole = user.getUserRole();
        if (userRole == null) {
            return false;
        }

        // 4. 比较
        return "ROLE_ADMIN".equals(userRole);
    }
}