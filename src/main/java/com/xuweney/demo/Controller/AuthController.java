package  com.xuweney.demo.Controller;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController             // = @Controller + @Responsebody
@RequestMapping("/auth")    // 定义路由接口
public class AuthController {
    @Autowired
    private UserService userService;    //注入service层服务

    @Autowired
    private PasswordEncoder passwordEncoder;    //密码加密服务

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username,@RequestParam String password) {
        Map<String, Object> result = new HashMap<>();

        //查询用户
        User user = userService.findUsername(username);
        if (user == null) {
            result.put("code",401);
            result.put("mes","用户不存在");
            return result;
        }

        //校验密码
        if (!passwordEncoder.matches(password,user.getPassword())) {
            result.put("code",401);
            result.put("mes","密码错误");
            return result;
        }

        //成功登录
        result.put("code",200);
        result.put("mes","登陆成功");
        result.put("data",Map.of("nickname",user.getNickname()));
        return result;
    }

}