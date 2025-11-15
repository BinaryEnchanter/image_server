package image.server.image_server.controller;

import image.server.image_server.controller.dto.LoginRequest;
import image.server.image_server.controller.dto.LoginResponse;
import image.server.image_server.controller.dto.RegisterDto;
import image.server.image_server.model.User;
import image.server.image_server.security.JwtUtil;
import image.server.image_server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 认证相关接口：register / login
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private image.server.image_server.service.ActionLogService actionLogService;

    /**
     * 简单注册（仅用于测试）。生产需要邮箱校验/验证码/更严格验证。
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDto dto) {
        try {
            User u = userService.register(dto.username, dto.email, dto.password);
            actionLogService.log(u.getUuid(), "register", null, "{\"username\":\"" + u.getUsername() + "\"}");
            return ResponseEntity.ok(u.getUuid());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * 登录：返回 JWT token + 基础用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
            UserDetails ud = (UserDetails) auth.getPrincipal();
            // ud.getUsername() 被我们设置为 uuid string（见 UserService）
            java.util.UUID uuid = java.util.UUID.fromString(ud.getUsername());
            String token = jwtUtil.generateToken(uuid);
            // fetch user for coins/username
            User u = userService.findByUuid(uuid).orElseThrow();
            actionLogService.log(u.getUuid(), "login", null, null);
            return ResponseEntity.ok(new LoginResponse(token, u.getUuid(), u.getUsername(), u.getCoins()));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body("invalid credentials");
        }
    }
}
