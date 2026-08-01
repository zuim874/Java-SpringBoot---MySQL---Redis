package com.xuweney.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.xuweney.demo.util.redis.RedisUtil;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    public UserService(UserMapper userMapper,
                       RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
    }

    //根据用户名查询用户（登录用，含 Redis 缓存）
    public User findUsernameforlogin(String username) {
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:login:active:" + username;

        // 第 1 步：先从 Redis 查，有则直接返回
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        //QueryWrapper<实体> wrapper = new QueryWrapper<>();创造一个条件构造器
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        //wrapper.eq("数据表字段名",传入参数);用条件构造器判断值是否相等
        wrapper.eq("username",username);
        User user = userMapper.selectOne(wrapper);

        // 第 3 步：查到了就写入 Redis（10 分钟过期，避免数据长期不一致）
        if (user != null) {
            redisUtil.set(cacheKey, user, 10, TimeUnit.MINUTES);
        }

        return user;
    }

    //根据用户名查询用户（包含已逻辑删除的）
    public User findUsername(String username) {
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:login:all:" + username;

        // redis查询
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // redis没有，查mysql
        User user = userMapper.findByUsernameAll(username);

        // 写入redis(10分钟过期)
        if (user != null) {
            redisUtil.set(cacheKey, user, 10, TimeUnit.MINUTES);
        }
        return user;
    }

    //查询删除用户
    public User findUsernameforRecover(String username) {
        String cacheKey = "demo:user:recover:" + username;

        // redis查询
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // redis没有，查mysql
        User user = userMapper.findByUsernameDeleted(username);

        // 写入redis(10分钟过期)
        if (user != null) {
            redisUtil.set(cacheKey, user, 10, TimeUnit.MINUTES);
        }
        return user;
    }

    //根据昵称查询用户（含 Redis 缓存）
    @SuppressWarnings("unchecked")
    public List<User> findNickname(String nickname) {
        String cacheKey = "demo:user:nickname:" + nickname;

        // 第 1 步：先从 Redis 查
        List<User> cached = (List<User>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("nickname", nickname);
        List<User> list = userMapper.selectList(wrapper);

        // 第 3 步：写入 Redis（5 分钟过期，列表缓存允许短暂不一致）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list, 5, TimeUnit.MINUTES);
        }
        return list;
    }

    //根据用户状态查询用户（含 Redis 缓存）
    @SuppressWarnings("unchecked")
    public List<User> findStatus(String status) {
        String cacheKey = "demo:user:status:" + status;

        // 第 1 步：先从 Redis 查
        List<User> cached = (List<User>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        List<User> list = userMapper.selectList(wrapper);

        // 第 3 步：写入 Redis（5 分钟过期）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list, 5, TimeUnit.MINUTES);
        }
        return list;
    }

    //根据用户ID查询用户（含 Redis 缓存）
    public User getById(Long id) {
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:id:" + id;

        // 第 1 步：先从 Redis 查
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        User user = userMapper.selectOne(wrapper);

        // 第 3 步：查到了就写入 Redis（10 分钟过期）
        if (user != null) {
            redisUtil.set(cacheKey, user, 10, TimeUnit.MINUTES);
        }
        return user;
    }

    //检查邮箱是否已存在（含 Redis 缓存）
    public boolean isEmailExist(String email) {
        String cacheKey = "demo:user:email:" + email;

        // 第 1 步：先从 Redis 查
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return true;
        }

        // 第 2 步：Redis 没有，查 MySQL
        User user = userMapper.findByEmail(email);

        // 第 3 步：存在则写入 Redis（10 分钟过期）
        if (user != null) {
            redisUtil.set(cacheKey, user, 10, TimeUnit.MINUTES);
            return true;
        }
        return false;
    }

    //保存用户
    public boolean save(User user) {
        String username = user.getUsername();
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:login:active:" + username;
        boolean result = userMapper.insert(user) > 0;

        redisUtil.set(cacheKey, user, 10, TimeUnit.MINUTES);

        return result;
    }

    //删除用户(逻辑删除，同时清理所有关联缓存)
    public boolean deleteById(Long id) {
        // 先查出用户信息，用于删除缓存
        User user = userMapper.selectById(id);
        boolean result = userMapper.deleteById(id) > 0;

        if (result && user != null) {
            String username = user.getUsername();
            // 清除该用户关联的所有缓存
            redisUtil.delete("demo:user:login:active:" + username);
            redisUtil.delete("demo:user:login:all:" + username);
            redisUtil.delete("demo:user:recover:" + username);
            redisUtil.delete("demo:user:id:" + id);
            if (user.getEmail() != null) {
                redisUtil.delete("demo:user:email:" + user.getEmail());
            }
        }
        return result;
    }

    //恢复用户(根据ID，恢复后刷新缓存)
    public boolean recoverById(Long id) {
        boolean result = userMapper.recoverById(id) > 0;
        if (result) {
            User user = userMapper.selectById(id);
            if (user != null) {
                String username = user.getUsername();
                // 清除旧的关联缓存（recover 和 all 中的数据已过时）
                redisUtil.delete("demo:user:recover:" + username);
                redisUtil.delete("demo:user:login:all:" + username);
                redisUtil.delete("demo:user:id:" + id);

                // 恢复后重新写入 active 缓存（用户已可登录）
                String cacheKey = "demo:user:login:active:" + username;
                redisUtil.set(cacheKey, user, 10, TimeUnit.MINUTES);
            }
        }
        return result;
    }

    //检查权限（复用 findUsername 的 Redis 缓存，避免额外数据库查询）
    public boolean isAdmin(String username) {
        // 1. 查询用户（含 Redis 缓存）
        User user = findUsername(username);

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