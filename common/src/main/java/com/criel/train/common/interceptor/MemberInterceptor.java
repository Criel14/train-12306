package com.criel.train.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.properties.JwtProperties;
import com.criel.train.common.resp.MemberLoginResp;
import com.criel.train.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户登录拦截器
 */
@Component
public class MemberInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(MemberInterceptor.class);

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 在请求处理之前进行调用：存储用户信息到ThreadLocal
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取header的token参数
        String token = request.getHeader("token");
        if (StrUtil.isNotBlank(token)) {
            LOG.info("获取会员登录token：{}", token);
            JSONObject loginMember = JwtUtil.getJSONObject(token, jwtProperties.getMemberSecretKey());
            LOG.info("当前登录会员：{}", loginMember);
            MemberLoginResp memberLoginResp = JSONUtil.toBean(loginMember, MemberLoginResp.class);
            LoginMemberContext.setMember(memberLoginResp);
        }
        return true;
    }

}
