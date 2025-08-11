package com.indigo.security.view;

import com.indigo.core.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OAuth2视图处理器
 * 处理OAuth2.0相关的视图响应
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
public class OAuth2ViewHandler {

    /**
     * 处理未登录视图
     *
     * @return 未登录响应
     */
    public Result<String> getNotLoginView() {
        log.info("用户未登录，返回登录提示");
        return Result.error("请先登录", "NOT_LOGIN");
    }

    /**
     * 处理授权确认视图
     *
     * @return 授权确认响应
     */
    public Result<String> getConfirmView() {
        log.info("返回授权确认视图");
        return Result.success("请确认授权");
    }

    /**
     * 处理错误视图
     *
     * @return 错误响应
     */
    public Result<String> getErrorView() {
        log.info("返回错误视图");
        return Result.error("授权过程中发生错误");
    }

    /**
     * 处理服务器错误视图
     *
     * @return 服务器错误响应
     */
    public Result<String> getServerErrorView() {
        log.info("返回服务器错误视图");
        return Result.error("服务器内部错误");
    }
} 