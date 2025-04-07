package com.criel.train.business.service;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 布隆过滤器服务，用于在多个地方操作同一个布隆过滤器
 */
@Service
public class BloomFilterService {

    private static final Logger LOG = LoggerFactory.getLogger(BloomFilterService.class);


    private final RBloomFilter<String> bloomFilter;

    public BloomFilterService(RedissonClient redissonClient) {
        // 获取名为 "trainBloomFilter" 的布隆过滤器
        String bloomFilterName = "trainBloomFilter";
        this.bloomFilter = redissonClient.getBloomFilter(bloomFilterName);
        // 初始化布隆过滤器：预期数据量根据业务情况设置，这里 1000 个组合，误判率设为 1%
        bloomFilter.tryInit(1000L, 0.01);
    }

    /**
     * 添加一个 Key 到布隆过滤器中
     */
    public void addTicketKey(String ticketKey) {
        bloomFilter.add(ticketKey);
    }

    /**
     * 判断一个 Key 是否存在于布隆过滤器中
     */
    public boolean contains(String ticketKey) {
        return bloomFilter.contains(ticketKey);
    }
}

