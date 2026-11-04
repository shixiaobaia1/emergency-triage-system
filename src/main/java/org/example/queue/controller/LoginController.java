package org.example.queue.controller;

import org.example.queue.context.UserContext;
import org.example.queue.entity.User;
import org.example.queue.service.LoginService;
import org.example.queue.vo.LoginRequest;
import org.example.queue.vo.LoginVO;
import org.example.queue.vo.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 登录 Controller
 * FIX: 原实现用 GET + query string 返回纯 String，改成 POST + JSON body + Result<LoginVO>
 */
@RestController
@RequestMapping("/login")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * 登录接口
     * 请求体 JSON：{"username":"admin","password":"123456"}
     * 成功返回 token，后续所有 /queue/** 请求必须带 header: X-Token: 你的token
     */
    @PostMapping
    public Result<LoginVO> login(@RequestBody LoginRequest req) {
        LoginVO vo = loginService.login(req.getUsername(), req.getPassword());
        if(vo == null) {
            return Result.fail(401, "用户名或密码错误");
        }
        return Result.success(vo);
    }

    /**
     * 登出接口
     * 需要带 X-Token header
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("X-Token") String token) {
        loginService.logout(token);
        return Result.success("已登出");
    }

    /**
     * 获取当前登录用户信息（鉴权拦截器放行 /login 后也会进拦截器，这个接口能拿到当前用户）
     */
    @GetMapping("/current")
    public Result<User> current() {
        User user = UserContext.get();
        if(user == null) {
            return Result.fail(401, "未登录");
        }
        // FIX: 不要把 password 返回给前端
        user.setPassword(null);
        return Result.success(user);
    }
}
