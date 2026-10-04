package com.aiuse.medreminder.service;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.entity.AppUser;
import com.aiuse.medreminder.mapper.AppUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 用户账号服务：注册、登录校验、密码加盐哈希。
 *
 * <p>密码用 JDK 自带的 PBKDF2WithHmacSHA256 存储（迭代 12 万次 + 随机盐），
 * 数据库里不保存明文；这样即使数据库泄露也拿不到原始密码。</p>
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserMapper userMapper;

    public UserService(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 注册：用户名唯一，注册成功后自动接管历史演示数据（无归属的档案）。 */
    public AppUser register(String username, String rawPassword, String displayName) {
        String name = username == null ? "" : username.trim();
        if (userMapper.findByUsername(name).isPresent()) {
            throw BizException.badRequest("该用户名已被注册，请换一个");
        }
        // 只有"第一个注册的账号"才接管历史演示数据，之后的注册都是全新账号
        boolean firstUser = userMapper.countUsers() == 0;

        AppUser user = new AppUser();
        user.setUsername(name);
        user.setDisplayName(StringUtils.hasText(displayName) ? displayName.trim() : name);
        user.setPasswordHash(hash(rawPassword));
        userMapper.insert(user);

        if (firstUser) {
            int claimed = userMapper.claimOrphanElders(user.getId());
            if (claimed > 0) {
                log.info("首个账号 {} 接管了 {} 条历史老人档案", user.getUsername(), claimed);
            }
        }
        return user;
    }

    public AppUser login(String username, String rawPassword) {
        AppUser user = userMapper.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> BizException.badRequest("用户名或密码错误"));
        if (!verify(rawPassword, user.getPasswordHash())) {
            throw BizException.badRequest("用户名或密码错误");
        }
        return user;
    }

    public AppUser requireById(long id) {
        return userMapper.findById(id)
                .orElseThrow(() -> new BizException(401, "账号不存在或已注销"));
    }

    /** 生成 "迭代次数:盐:哈希" 格式的字符串。 */
    private static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = pbkdf2(rawPassword, salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(key);
    }

    /** 校验密码，使用定长比较避免时间侧信道。 */
    private static boolean verify(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf2(rawPassword, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String rawPassword, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }
}
