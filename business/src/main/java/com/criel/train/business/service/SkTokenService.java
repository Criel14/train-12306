package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.criel.train.business.enumeration.RedisKeyPreEnum;
import com.criel.train.business.mapper.customer.SkTokenMapperCustomer;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.domain.generated.SkToken;
import com.criel.train.business.domain.generated.SkTokenExample;
import com.criel.train.business.mapper.SkTokenMapper;
import com.criel.train.business.req.SkTokenQueryReq;
import com.criel.train.business.req.SkTokenSaveReq;
import com.criel.train.business.resp.SkTokenQueryResp;
import jakarta.annotation.Resource;
import org.checkerframework.checker.units.qual.A;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SkTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(SkTokenService.class);

    @Resource
    private SkTokenMapper skTokenMapper;

    @Autowired
    private DailyTrainSeatService dailyTrainSeatService;

    @Autowired
    private DailyTrainStationService dailyTrainStationService;

    @Autowired
    private SkTokenMapperCustomer skTokenMapperCustomer;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void save(SkTokenSaveReq req) {
        DateTime now = DateTime.now();
        SkToken skToken = BeanUtil.copyProperties(req, SkToken.class);
        if (ObjectUtil.isNull(skToken.getId())) {
            skToken.setId(SnowflakeUtil.getSnowflakeNextId());
            skToken.setCreateTime(now);
            skToken.setUpdateTime(now);
            skTokenMapper.insert(skToken);
        } else {
            skToken.setUpdateTime(now);
            skTokenMapper.updateByPrimaryKey(skToken);
        }
    }

    public PageResp<SkTokenQueryResp> queryList(SkTokenQueryReq req) {
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.setOrderByClause("id desc");
        SkTokenExample.Criteria criteria = skTokenExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<SkToken> skTokenList = skTokenMapper.selectByExample(skTokenExample);

        PageInfo<SkToken> pageInfo = new PageInfo<>(skTokenList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<SkTokenQueryResp> list = BeanUtil.copyToList(skTokenList, SkTokenQueryResp.class);

        PageResp<SkTokenQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        skTokenMapper.deleteByPrimaryKey(id);
    }


    /**
     * 生成对应日期、对应车次的令牌数据
     *
     * @param date
     * @param trainCode
     */
    public void genDaily(Date date, String trainCode) {
        LOG.info("删除日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        skTokenMapper.deleteByExample(skTokenExample);

        DateTime now = DateTime.now();
        SkToken skToken = new SkToken();
        skToken.setDate(date);
        skToken.setTrainCode(trainCode);
        skToken.setId(SnowflakeUtil.getSnowflakeNextId());
        skToken.setCreateTime(now);
        skToken.setUpdateTime(now);

        int seatCount = dailyTrainSeatService.countSeat(date, trainCode);

        long stationCount = dailyTrainStationService.countByTrainCode(date, trainCode);

        // 一趟火车最多可以卖（seatCount * stationCount）张火车票
        int count = (int) (seatCount * stationCount);
        skToken.setCount(count);
        LOG.info("车次【{}】初始生成令牌数：{}", trainCode, count);

        skTokenMapper.insert(skToken);
    }

    /**
     * 获取并扣减令牌，返回是否获取成功
     * 扣减成功返回true，失败返回false，如果有重复获取的情况，则会抛出异常
     *
     * @param date
     * @param trainCode
     * @param memberId
     * @return
     */
    public boolean validSkToken(Date date, String trainCode, Long memberId) {
        LOG.info("会员{}尝试获取{}日期{}车次的令牌", memberId, DateUtil.formatDate(date), trainCode);

        // 使用分布式锁来获取令牌
        String tokenLockKey = RedisKeyPreEnum.SK_TOKEN.getCode() + trainCode + ":" + DateUtil.formatDate(date) + ":" + memberId;
        RLock rLock = redissonClient.getLock(tokenLockKey);
        boolean locked = false;
        try {
            locked = rLock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                LOG.info("会员{}获取{}日期{}车次的令牌失败，锁被占用", memberId, DateUtil.formatDate(date), trainCode);

                throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 不要释放锁，在锁定时间内都不可再次获取令牌

        LOG.info("会员{}获取{}日期{}车次的令牌成功", memberId, DateUtil.formatDate(date), trainCode);

        String countLockKey = RedisKeyPreEnum.SK_TOKEN_COUNT.getCode() + trainCode + ":" + DateUtil.formatDate(date);
        Long tokenCountResult = (Long) redisTemplate.opsForValue().get(countLockKey);
        if (tokenCountResult != null) {
            // 如果缓存中有数据
            long tokenCount = redisTemplate.opsForValue().decrement(countLockKey, 1);
            if (tokenCount >= 0) {
                // 刷新缓存时间
                redisTemplate.expire(countLockKey, 60, TimeUnit.SECONDS);
                // 每5次更新一次数据库
                // TODO 这里最好改成定时任务
                if (tokenCount % 5 == 0) {
                    skTokenMapperCustomer.decreaseCount(date, trainCode, 5);
                }
                return true;
            } else {
                return false;
            }
        } else {
            // 如果缓存中没有数据
            // 先查询是否存在该日期该车次的令牌
            SkTokenExample skTokenExample = new SkTokenExample();
            skTokenExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
            List<SkToken> skTokenList = skTokenMapper.selectByExample(skTokenExample);
            if (skTokenList.isEmpty()) {
                throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
            }

            // 缓存
            Integer count = skTokenList.get(0).getCount();
            if (skTokenList.get(0).getCount() <= 0){
                return false;
            }
            redisTemplate.opsForValue().set(countLockKey, count, 60, TimeUnit.SECONDS);
        }
        return true;
    }
}
