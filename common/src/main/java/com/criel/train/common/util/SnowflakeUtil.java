package com.criel.train.common.util;

import cn.hutool.core.util.IdUtil;

/**
 * 封装hutool雪花算法
 */
public class SnowflakeUtil {

    /**
     * TODO 以下2个值每台机器都不一样，后面可能需要配置到数据库，启动时从数据库读取
     */
    // 数据中心
    private static long dataCenterId = 1;
    // 机器标识
    private static long workerId = 1;

    // 用Hutool工具的雪花算法生成id
    public static long getSnowflakeNextId() {
        return IdUtil.getSnowflake(workerId, dataCenterId).nextId();
    }

    public static String getSnowflakeNextIdStr() {
        return IdUtil.getSnowflake(workerId, dataCenterId).nextIdStr();
    }
}
