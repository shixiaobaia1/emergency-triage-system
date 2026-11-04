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
     * 登出接口——必须带有效 token（拦截器已校验）
     * FIX: 现在 /login/logout 不再被拦截器放行，token 无效会直接返回 401
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("X-Token") String token) {
        // 拦截器已经保证 token 有效（能进 Controller 就说明 token 没问题）
        loginService.logout(token);
        return Result.success("已登出");
    }

    /**
     * 获取当前登录用户信息
     * FIX: /login/current 也需要 token 校验（不放行），拦截器把 user 放进 UserContext 后这里直接拿
     * FIX: 返回副本而不是原对象——原对象的 password 不能在这里被置 null，否则会污染 TokenUtil 内存里的 User
     */
    @GetMapping("/current")
    public Result<User> current() {
        User user = UserContext.get();
        if(user == null) {
            return Result.fail(401, "未登录");
        }
        // FIX: new 一个副本返回，不修改内存里原对象的任何字段（原对象的 password 不能被置 null！）
        User safeCopy = new User();
        safeCopy.setUsername(user.getUsername());
        safeCopy.setRole(user.getRole());
        // password 不拷贝——脱敏
        return Result.success(safeCopy);
    }
}
