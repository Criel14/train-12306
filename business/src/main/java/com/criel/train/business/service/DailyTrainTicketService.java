package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.criel.train.business.domain.RedisData;
import com.criel.train.business.domain.generated.DailyTrain;
import com.criel.train.business.domain.generated.TrainStation;
import com.criel.train.business.enumeration.SeatTypeEnum;
import com.criel.train.business.enumeration.TrainTypeEnum;
import com.criel.train.common.constant.RedisConstant;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.domain.generated.DailyTrainTicket;
import com.criel.train.business.domain.generated.DailyTrainTicketExample;
import com.criel.train.business.mapper.DailyTrainTicketMapper;
import com.criel.train.business.req.DailyTrainTicketQueryReq;
import com.criel.train.business.resp.DailyTrainTicketQueryResp;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DailyTrainTicketService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainTicketService.class);

    private static final ExecutorService CACHE_EXECUTOR = Executors.newFixedThreadPool(10);

    @Autowired
    private DailyTrainTicketMapper dailyTrainTicketMapper;

    @Autowired
    private TrainStationService trainStationService;

    @Autowired
    private DailyTrainSeatService dailyTrainSeatService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BloomFilterService bloomFilterService;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 管理员余票查询
     *
     * @param req
     * @return
     */
    public PageResp<DailyTrainTicketQueryResp> queryListAdmin(DailyTrainTicketQueryReq req) {
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.setOrderByClause("id desc");
        DailyTrainTicketExample.Criteria criteria = dailyTrainTicketExample.createCriteria();

        LOG.info("DailyTrainTicketQueryReq: {}", req.toString());

        if (req.getDate() != null) {
            criteria.andDateEqualTo(req.getDate());
        }
        if (req.getTrainCode() != null && !req.getTrainCode().isEmpty()) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }
        if (req.getStart() != null && !req.getStart().isEmpty()) {
            criteria.andStartEqualTo(req.getStart());
        }
        if (req.getEnd() != null && !req.getEnd().isEmpty()) {
            criteria.andEndEqualTo(req.getEnd());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);

        PageInfo<DailyTrainTicket> pageInfo = new PageInfo<>(dailyTrainTicketList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainTicketQueryResp> list = BeanUtil.copyToList(dailyTrainTicketList, DailyTrainTicketQueryResp.class);

        PageResp<DailyTrainTicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    /**
     * 用户余票查询
     * 用布隆过滤器防止缓存穿透，用逻辑过期防止缓存击穿
     *
     * @param req
     * @return
     */
    public PageResp<DailyTrainTicketQueryResp> queryListMember(DailyTrainTicketQueryReq req) {
        LOG.info("DailyTrainTicketQueryReq: {}", req.toString());

        Date reqDate = req.getDate();
        String reqStart = req.getStart();
        String reqEnd = req.getEnd();

        // 默认分页结果为空
        PageResp<DailyTrainTicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(0L);
        pageResp.setList(null);

        // key格式daily:ticket:{date}:from:{start}:to:{end}
        String key = RedisConstant.DAILY_TRAIN_KEY + DateUtil.formatDate(reqDate) + ":from:" + reqStart + ":to:" + reqEnd;
        LOG.info("queryListMember：redis的key: {}", key);
        // 校验布隆过滤器
        if (!bloomFilterService.contains(key)) {
            LOG.info("queryListMember：布隆过滤器拦截");
            return pageResp;
        }

        // 查询redis
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        // 如果redis中不存在也返回空
        if (StrUtil.isBlank(jsonStr)) {
            LOG.info("queryListMember：Redis内容为空");
            return pageResp;
        }

        // 格式化数据
        RedisData redisData = JSONUtil.toBean(jsonStr, RedisData.class);
        JSONArray data = (JSONArray) redisData.getData();
        List<DailyTrainTicket> tickets = JSONUtil.toList(data, DailyTrainTicket.class);

        // 校验过期时间
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isBefore(LocalDateTime.now())) {
            LOG.info("queryListMember：redis中的数据数据已过期");

            // 过期则获取分布式锁，开启线程去查数据库
            RLock rLock = redissonClient.getLock(RedisConstant.LOCK_KEY_PREFIX + key);
            boolean isLocked = false;
            try {
                isLocked = rLock.tryLock(10, TimeUnit.SECONDS);
                if (isLocked) {
                    CACHE_EXECUTOR.submit(() -> updateRedisCache(reqDate, reqStart, reqEnd));
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (isLocked && rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                }
            }
        } else {
            LOG.info("queryListMember：redis中的数据数据未过期");
        }

        // 数据过期或未过期都直接返回
        return buildPage(tickets, req.getPage(), req.getSize());
    }

    /**
     * 更新redis缓存信息
     *
     * @param reqDate
     * @param reqStart
     * @param reqEnd
     */
    public void updateRedisCache(Date reqDate, String reqStart, String reqEnd) {
        // 查询数据库
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.setOrderByClause("train_code desc");
        DailyTrainTicketExample.Criteria criteria = dailyTrainTicketExample.createCriteria();

        // 前端应该3个参数都会传来的，这里只是做了健壮性
        if (reqDate != null) {
            criteria.andDateEqualTo(reqDate);
        }
        if (reqStart != null && !reqStart.isEmpty()) {
            criteria.andStartEqualTo(reqStart);
        }
        if (reqEnd != null && !reqEnd.isEmpty()) {
            criteria.andEndEqualTo(reqEnd);
        }

        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);

        // 封装逻辑过期
        RedisData newredisData = new RedisData(
                LocalDateTime.now().plusSeconds(RedisConstant.EXPIRE_TIME_SECOND),
                dailyTrainTicketList
        );
        // 缓存redis
        String key = RedisConstant.DAILY_TRAIN_KEY + DateUtil.formatDate(reqDate) + ":from:" + reqStart + ":to:" + reqEnd;
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(newredisData));
        LOG.info("queryListMember：已添加新的缓存数据");
    }

    /**
     * 构建分页返回值
     *
     * @param dailyTrainTicketList 完整的dailyTrainTicket数据
     * @param page
     * @param size
     * @return
     */
    private PageResp<DailyTrainTicketQueryResp> buildPage(List<DailyTrainTicket> dailyTrainTicketList, int page, int size) {
        // page从1开始，转换为从0开始
        page--;

        // 计算起始索引和结束索引
        int totalSize = dailyTrainTicketList.size();
        int fromIndex = page * size;
        int toIndex = Math.min((page + 1) * size, totalSize);

        // 检查起始索引是否合法
        if (fromIndex > totalSize) {
            // 如果起始索引超出范围，返回空列表
            List<DailyTrainTicketQueryResp> emptyList = new ArrayList<>();
            PageResp<DailyTrainTicketQueryResp> pageResp = new PageResp<>();
            pageResp.setTotal((long) totalSize);
            pageResp.setList(emptyList);
            return pageResp;
        }

        // 使用安全的索引范围获取子列表
        List<DailyTrainTicket> subList = dailyTrainTicketList.subList(fromIndex, toIndex);
        List<DailyTrainTicketQueryResp> list = BeanUtil.copyToList(subList, DailyTrainTicketQueryResp.class);

        PageResp<DailyTrainTicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal((long) totalSize);
        pageResp.setList(list);
        return pageResp;
    }

    /**
     * 根据唯一键查询
     *
     * @param date
     * @param trainCode
     * @param start
     * @param end
     * @return
     */
    public DailyTrainTicket selectByUnique(Date date, String trainCode, String start, String end) {
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria()
                .andDateEqualTo(date)
                .andTrainCodeEqualTo(trainCode)
                .andStartEqualTo(start)
                .andEndEqualTo(end);
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);
        if (!dailyTrainTicketList.isEmpty()) {
            return dailyTrainTicketList.get(0);
        } else {
            return null;
        }
    }

    /**
     * 生成对应日期、对应车次的余票数据
     *
     * @param date
     * @param dailyTrain
     */
    @Transactional
    public void genDaily(Date date, DailyTrain dailyTrain) {
        String trainCode = dailyTrain.getCode();
        String trainType = dailyTrain.getType();

        // 删除原有数据
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        dailyTrainTicketMapper.deleteByExample(dailyTrainTicketExample);

        // 生成
        LOG.info("开始生成{}的{}车次的余票信息", DateUtil.formatDate(date), trainCode);
        Date now = new Date();

        // 生成车站区间信息
        List<TrainStation> trainStationList = trainStationService.selectByTrainCode(trainCode);
        if (trainStationList == null || trainStationList.isEmpty()) {
            LOG.info("{}车次没有车站信息，无法生成每日余票数据", trainCode);
            return;
        }

        // 获取该车次该日期，各座位类型的总数
        int ydzCount = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.YDZ.getCode());
        int edzCount = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.EDZ.getCode());
        int rwCount = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.RW.getCode());
        int ywCount = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.YW.getCode());

        // 车次类型票价系数
        BigDecimal trainTypePriceRate = TrainTypeEnum.getPriceFactor(trainType);

        for (int start = 0; start < trainStationList.size(); start++) {
            TrainStation startStation = trainStationList.get(start);
            // start到end车站区间距离
            BigDecimal distance = BigDecimal.ZERO;
            for (int end = start + 1; end < trainStationList.size(); end++) {
                TrainStation endStation = trainStationList.get(end);
                DailyTrainTicket dailyTrainTicket = new DailyTrainTicket();
                dailyTrainTicket.setId(SnowflakeUtil.getSnowflakeNextId());
                dailyTrainTicket.setDate(date);
                dailyTrainTicket.setTrainCode(trainCode);
                dailyTrainTicket.setStart(startStation.getName());
                dailyTrainTicket.setStartPinyin(startStation.getNamePinyin());
                dailyTrainTicket.setStartTime(startStation.getOutTime());
                dailyTrainTicket.setStartIndex(startStation.getIndex());
                dailyTrainTicket.setEnd(endStation.getName());
                dailyTrainTicket.setEndPinyin(endStation.getNamePinyin());
                dailyTrainTicket.setEndTime(endStation.getInTime());
                dailyTrainTicket.setEndIndex(endStation.getIndex());
                dailyTrainTicket.setCreateTime(now);
                dailyTrainTicket.setUpdateTime(now);
                dailyTrainTicket.setYdz(ydzCount);
                dailyTrainTicket.setEdz(edzCount);
                dailyTrainTicket.setRw(rwCount);
                dailyTrainTicket.setYw(ywCount);

                // 票价 = 车站区间距离 * 座位类型价格 * 车辆类型价格系数 （简化，无阶梯计价）
                distance = distance.add(endStation.getKm()); // getKm()是上一站到本站的距离
                BigDecimal ydzPrice = distance.multiply(SeatTypeEnum.YDZ.getPrice()).multiply(trainTypePriceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal edzPrice = distance.multiply(SeatTypeEnum.EDZ.getPrice()).multiply(trainTypePriceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal rwPrice = distance.multiply(SeatTypeEnum.RW.getPrice()).multiply(trainTypePriceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal ywPrice = distance.multiply(SeatTypeEnum.YW.getPrice()).multiply(trainTypePriceRate).setScale(2, RoundingMode.HALF_UP);
                // 除去没有的座位类型
                if (ydzCount == -1) {
                    ydzPrice = BigDecimal.ZERO;
                }
                if (edzCount == -1) {
                    edzPrice = BigDecimal.ZERO;
                }
                if (rwCount == -1) {
                    rwPrice = BigDecimal.ZERO;
                }
                if (ywCount == -1) {
                    ywPrice = BigDecimal.ZERO;
                }
                dailyTrainTicket.setYdzPrice(ydzPrice);
                dailyTrainTicket.setEdzPrice(edzPrice);
                dailyTrainTicket.setRwPrice(rwPrice);
                dailyTrainTicket.setYwPrice(ywPrice);

                dailyTrainTicketMapper.insert(dailyTrainTicket);
            }
        }

        LOG.info("结束生成{}的{}车次的余票信息", DateUtil.formatDate(date), trainCode);
    }

    /**
     * 生成每日余票信息进redis，同时保存到布隆过滤器中;
     * redis中key为daily:ticket:{date}:from:{start}:to:{end}，value为List泛型dailyTrainTicket
     * 即List保存date日期，从start站到end站的所有dailyTrainTicket
     *
     * @param date
     */
    public void genDailyRedis(Date date) {
        // 获取date的所有车票信息
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria().andDateEqualTo(date);
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);

        // 按照【起点站 : 终点站】来分组
        Map<String, List<DailyTrainTicket>> dailyTrainTicketGroupMap = dailyTrainTicketList.stream().collect(
                Collectors.groupingBy(dailyTrainTicket -> "from:" + dailyTrainTicket.getStart() + ":to:" + dailyTrainTicket.getEnd())
        );

        // 保存到redis和布隆过滤器
        for (Map.Entry<String, List<DailyTrainTicket>> entry : dailyTrainTicketGroupMap.entrySet()) {
            String key = RedisConstant.DAILY_TRAIN_KEY + DateUtil.formatDate(date) + ":" + entry.getKey();
            List<DailyTrainTicket> tickets = entry.getValue();
            // 封装逻辑过期
            RedisData redisData = new RedisData(
                    LocalDateTime.now().plusSeconds(RedisConstant.EXPIRE_TIME_SECOND),
                    tickets
            );
            // 缓存redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
            // 缓存布隆过滤器
            bloomFilterService.addTicketKey(key);
        }
    }
}
