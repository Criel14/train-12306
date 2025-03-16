package com.criel.train.gateway.config;

import com.criel.train.gateway.config.properties.JwtProperties;
import com.criel.train.gateway.config.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 登录会员jwt拦截器
 */
@Component
public class LoginMemberFilter implements Ordered, GlobalFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoginMemberFilter.class);

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 排除不需要拦截的请求：管理员、测试接口、登录接口、获取验证码接口
        if (path.contains("/admin")
                || path.contains("/test-connect")
                || path.contains("/member/member/login")
                || path.contains("/member/member/code")) {
            LOG.info("排除不需要登录验证的请求：{}", path);
            return chain.filter(exchange);
        } else {
            LOG.info("拦截到需要登录验证的请求：{}", path);
        }
        // 获取header的token参数
        String token = exchange.getRequest().getHeaders().getFirst("token");
        LOG.info("会员登录验证开始，token：{}", token);
        if (token == null || token.isEmpty()) {
            LOG.info("token为空，请求被拦截");
            // 返回码401：无权限
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            // 中断请求
            return exchange.getResponse().setComplete();
        }

        // 校验token是否有效，包括token是否被改过，是否过期
        boolean validate = JwtUtil.validate(token, jwtProperties.getMemberSecretKey());
        if (validate) {
            LOG.info("token有效，放行该请求");
            return chain.filter(exchange);
        } else {
            LOG.warn("token无效，请求被拦截");
            // 返回码401：无权限
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 拦截器的优先级设置
     * 值越小，优先级越高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
