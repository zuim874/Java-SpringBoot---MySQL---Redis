package com.xuweney.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuweney.demo.Entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    //方法已经由basemapper自动生成
}
