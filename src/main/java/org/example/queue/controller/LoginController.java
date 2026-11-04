package org.example.queue.controller;

import org.example.queue.context.UserContext;
import org.example.queue.entity.User;
import org.example.queue.service.LoginService;
import org.example.queue.vo.LoginRequest;
import org.example.queue.vo.LoginVO;
import org.example.queue.vo.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     登录
     请求体 JSON：{"username":"admin","password":"123456"}
     成功返回 token，后续所有 /queue/** 请求请求头必须带: X-Token: token
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
     登出接口——带有效 token（拦截器已校验）
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("X-Token") String token) {
        loginService.logout(token);
        return Result.success("已登出");
    }

    /**
     * 获取当前登录用户信息
        拦截器把 user 放进 UserContext查信息去这里查
     */
    @GetMapping("/current")
    public Result<User> current() {
        User user = UserContext.get();
        if(user == null) {
            return Result.fail(401, "未登录");
        }
        //new 一个副本返回，不修改内存里原对象的任何字段
        User safeCopy = new User();
        safeCopy.setUsername(user.getUsername());
        safeCopy.setRole(user.getRole());
        return Result.success(safeCopy);
    }
}
