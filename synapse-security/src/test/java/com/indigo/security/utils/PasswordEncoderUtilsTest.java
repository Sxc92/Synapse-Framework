//package com.indigo.security.utils;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * 密码加密工具测试类
// *
// * @author 史偕成
// * @date 2025/01/08
// */
//@Slf4j
//@SpringBootTest
//public class PasswordEncoderUtilsTest {
//
//    @Autowired
//    private PasswordEncoderUtils passwordEncoderUtils;
//
//    @Test
//    public void testBCryptEncoding() {
//        String rawPassword = "testPassword123";
//        String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.BCRYPT);
//
//        assertNotNull(encodedPassword);
//        assertTrue(encodedPassword.startsWith("$2"));
//        assertTrue(passwordEncoderUtils.matches(rawPassword, encodedPassword, PasswordEncoderUtils.EncoderType.BCRYPT));
//
//        log.info("BCrypt加密测试通过: {}", encodedPassword);
//    }
//
//    @Test
//    public void testSCryptEncoding() {
//        String rawPassword = "testPassword123";
//        String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.SCRYPT);
//
//        assertNotNull(encodedPassword);
//        assertTrue(encodedPassword.startsWith("$s0$"));
//        assertTrue(passwordEncoderUtils.matches(rawPassword, encodedPassword, PasswordEncoderUtils.EncoderType.SCRYPT));
//
//        log.info("SCrypt加密测试通过: {}", encodedPassword);
//    }
//
//    @Test
//    public void testMD5Encoding() {
//        String rawPassword = "testPassword123";
//        String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.MD5);
//
//        assertNotNull(encodedPassword);
//        assertEquals(32, encodedPassword.length());
//        assertTrue(passwordEncoderUtils.matches(rawPassword, encodedPassword, PasswordEncoderUtils.EncoderType.MD5));
//
//        log.info("MD5加密测试通过: {}", encodedPassword);
//    }
//
//    @Test
//    public void testSHA256Encoding() {
//        String rawPassword = "testPassword123";
//        String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.SHA256);
//
//        assertNotNull(encodedPassword);
//        assertEquals(64, encodedPassword.length());
//        assertTrue(passwordEncoderUtils.matches(rawPassword, encodedPassword, PasswordEncoderUtils.EncoderType.SHA256));
//
//        log.info("SHA256加密测试通过: {}", encodedPassword);
//    }
//
//    @Test
//    public void testAutoDetection() {
//        String rawPassword = "testPassword123";
//
//        // BCrypt
//        String bcryptPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.BCRYPT);
//        assertTrue(passwordEncoderUtils.matchesAuto(rawPassword, bcryptPassword));
//
//        // SCrypt
//        String scryptPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.SCRYPT);
//        assertTrue(passwordEncoderUtils.matchesAuto(rawPassword, scryptPassword));
//
//        // MD5
//        String md5Password = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.MD5);
//        assertTrue(passwordEncoderUtils.matchesAuto(rawPassword, md5Password));
//
//        // SHA256
//        String sha256Password = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.SHA256);
//        assertTrue(passwordEncoderUtils.matchesAuto(rawPassword, sha256Password));
//
//        log.info("自动检测测试通过");
//    }
//
//    @Test
//    public void testPasswordStrength() {
//        // 弱密码
//        assertEquals(PasswordEncoderUtils.PasswordStrength.WEAK,
//            passwordEncoderUtils.checkPasswordStrength("123"));
//        assertEquals(PasswordEncoderUtils.PasswordStrength.WEAK,
//            passwordEncoderUtils.checkPasswordStrength("abc"));
//
//        // 中等密码
//        assertEquals(PasswordEncoderUtils.PasswordStrength.MEDIUM,
//            passwordEncoderUtils.checkPasswordStrength("abc123"));
//        assertEquals(PasswordEncoderUtils.PasswordStrength.MEDIUM,
//            passwordEncoderUtils.checkPasswordStrength("Abc123"));
//
//        // 强密码
//        assertEquals(PasswordEncoderUtils.PasswordStrength.STRONG,
//            passwordEncoderUtils.checkPasswordStrength("Abc123!"));
//        assertEquals(PasswordEncoderUtils.PasswordStrength.STRONG,
//            passwordEncoderUtils.checkPasswordStrength("Password123"));
//
//        // 很强密码
//        assertEquals(PasswordEncoderUtils.PasswordStrength.VERY_STRONG,
//            passwordEncoderUtils.checkPasswordStrength("Password123!"));
//        assertEquals(PasswordEncoderUtils.PasswordStrength.VERY_STRONG,
//            passwordEncoderUtils.checkPasswordStrength("MySecure@Pass123"));
//
//        log.info("密码强度检测测试通过");
//    }
//
//    @Test
//    public void testRandomPasswordGeneration() {
//        String password = passwordEncoderUtils.generateRandomPassword(12);
//
//        assertNotNull(password);
//        assertEquals(12, password.length());
//
//        // 检查密码强度
//        PasswordEncoderUtils.PasswordStrength strength = passwordEncoderUtils.checkPasswordStrength(password);
//        assertTrue(strength == PasswordEncoderUtils.PasswordStrength.STRONG ||
//                  strength == PasswordEncoderUtils.PasswordStrength.VERY_STRONG);
//
//        log.info("随机密码生成测试通过: {}", password);
//    }
//
//    @Test
//    public void testSaltGeneration() {
//        String salt = passwordEncoderUtils.generateSalt(16);
//
//        assertNotNull(salt);
//        assertTrue(salt.length() > 0);
//
//        // 生成多个盐，确保不重复
//        String salt2 = passwordEncoderUtils.generateSalt(16);
//        assertNotEquals(salt, salt2);
//
//        log.info("盐生成测试通过: {}", salt);
//    }
//
//    @Test
//    public void testDefaultEncoding() {
//        String rawPassword = "testPassword123";
//        String encodedPassword = passwordEncoderUtils.encode(rawPassword);
//
//        assertNotNull(encodedPassword);
//        assertTrue(encodedPassword.startsWith("$2")); // 默认使用BCrypt
//        assertTrue(passwordEncoderUtils.matches(rawPassword, encodedPassword));
//
//        log.info("默认加密测试通过: {}", encodedPassword);
//    }
//
//    @Test
//    public void testInvalidPassword() {
//        // 空密码
//        assertThrows(IllegalArgumentException.class, () -> {
//            passwordEncoderUtils.encode("");
//        });
//
//        assertThrows(IllegalArgumentException.class, () -> {
//            passwordEncoderUtils.encode(null);
//        });
//
//        // 空密码验证
//        assertFalse(passwordEncoderUtils.matches("", "any"));
//        assertFalse(passwordEncoderUtils.matches(null, "any"));
//        assertFalse(passwordEncoderUtils.matches("any", null));
//
//        log.info("无效密码处理测试通过");
//    }
//}
