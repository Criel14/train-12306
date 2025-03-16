package com.criel.train.common.interceptor;

import cn.hutool.core.util.RandomUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 日志拦截器
 */
@Component
public class LogInterceptor implements HandlerInterceptor {

    /**
     * 在请求处理之前进行调用：增加线程的日志流水号
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        MDC.put("LOG_ID", System.currentTimeMillis() + RandomUtil.randomString(3));
        return true;
    }

    /**
     * 在请求处理之后进行调用：删除线程的日志流水号，防止内存溢出问题
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove("LOG_ID");
    }

}
