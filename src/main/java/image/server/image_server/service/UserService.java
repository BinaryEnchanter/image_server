package image.server.image_server.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import image.server.image_server.model.User;
import image.server.image_server.repository.UserRepository;

/**
 * UserService 同时实现 UserDetailsService（Spring Security 用）
 */
@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 注册用户（简单版本）
    public User register(String username, String email, String plainPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("username exists");
        }
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(plainPassword));
        u.setCoins(100L); // 默认发放 100 金币以便测试
        return userRepository.save(u);
    }

    // login 在 AuthenticationManager 中处理，下面方法用于 JWT filter 中按 uuid 加载
    public User loadByUuidString(String uuidStr) {
        try {
            UUID id = UUID.fromString(uuidStr);
            return userRepository.findById(id).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // Spring Security: 根据 username 加载用户（用于认证）
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        // map to UserDetails
        List<GrantedAuthority> auth = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().toUpperCase()));
        return new org.springframework.security.core.userdetails.User(
                u.getUuid().toString(), u.getPasswordHash(), auth);
    }

    // 供 JwtFilter 用：通过 uuid 字符串加载 UserDetails
    public UserDetails loadUserByUuid(String uuidString) {
        User u = loadByUuidString(uuidString);
        if (u == null) return null;
        List<GrantedAuthority> auth = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().toUpperCase()));
        return new org.springframework.security.core.userdetails.User(u.getUuid().toString(), u.getPasswordHash(), auth);
    }

    public Optional<User> findByUuid(UUID uuid) {
        return userRepository.findById(uuid);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User save(User u) {
        return userRepository.save(u);
    }
    
    public boolean isAdmin(UUID uuid) {
        Optional<User> u = userRepository.findById(uuid);
        if (u.isPresent()) {
            return u.get().getRole().equals("admin");
        }

        return false;

    }

    public boolean isBlacklisted(UUID uuid) {
        Optional<User> u = userRepository.findById(uuid);
        return u.map(x -> "blacklist".equalsIgnoreCase(x.getRole())).orElse(false);
    }

    public User updateRole(UUID uuid, String role) {
        User u = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("user not found"));
        String r = role == null ? "" : role.toLowerCase();
        if (!r.equals("user") && !r.equals("admin") && !r.equals("blacklist")) {
            throw new RuntimeException("invalid role");
        }
        u.setRole(r);
        return userRepository.save(u);
    }

      // 编辑用户名
    public User updateUsername(UUID uuid, String newUsername) {
        // 用户名是否已被占用
        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new RuntimeException("username exists");
        }

        User u = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("user not found"));

        u.setUsername(newUsername);
        return userRepository.save(u);
    }

    // 编辑邮箱
    public User updateEmail(UUID uuid, String newEmail) {
        User u = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("user not found"));

        u.setEmail(newEmail);
        return userRepository.save(u);
    }

    // 编辑密码
    public User updatePassword(UUID uuid, String newPlainPassword) {
        User u = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("user not found"));

        u.setPasswordHash(passwordEncoder.encode(newPlainPassword));
        return userRepository.save(u);
    }
}
