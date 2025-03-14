package com.criel.train.common.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * JWT工具类
 */
public class JwtUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JwtUtil.class);

    /**
     * 生成token
     * @param payload 数据，例如："id": 123
     * @param key 签名key
     * @param exp 过期时间
     * @return
     */
    public static String createToken(Map<String, Object> payload, String key, int exp) {
        DateTime now = DateTime.now();
        // 过期时间
        DateTime expTime = now.offsetNew(DateField.MILLISECOND, exp);
        // 设置签发时间
        payload.put(JWTPayload.ISSUED_AT, now);
        // 设置过期时间
        payload.put(JWTPayload.EXPIRES_AT, expTime);
        // 设置生效时间
        payload.put(JWTPayload.NOT_BEFORE, now);
        String token = JWTUtil.createToken(payload, key.getBytes());
        LOG.info("生成JWT token：{}", token);
        return token;
    }

    /**
     * 校验token
     * @param token
     * @param key
     * @return
     */
    public static boolean validate(String token, String key) {
        JWT jwt = JWTUtil.parseToken(token).setKey(key.getBytes());
        // validate包含了verify
        boolean validate = jwt.validate(0);
        LOG.info("JWT token校验结果：{}", validate);
        return validate;
    }

    /**
     * 根据token获取原始内容
     * @param token
     * @param key
     * @return
     */
    public static JSONObject getJSONObject(String token, String key) {
        JWT jwt = JWTUtil.parseToken(token).setKey(key.getBytes());
        JSONObject payloads = jwt.getPayloads();
        payloads.remove(JWTPayload.ISSUED_AT);
        payloads.remove(JWTPayload.EXPIRES_AT);
        payloads.remove(JWTPayload.NOT_BEFORE);
        LOG.info("根据token获取原始内容：{}", payloads);
        return payloads;
    }
}
