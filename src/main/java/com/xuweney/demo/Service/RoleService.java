package com.xuweney.demo.Service;

import com.xuweney.demo.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
    @Autowired
    private UserMapper userMapper;
}
