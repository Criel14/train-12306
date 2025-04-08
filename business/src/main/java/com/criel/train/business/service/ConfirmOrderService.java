package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.criel.train.business.domain.generated.*;
import com.criel.train.business.dto.ConfirmOrderMQDto;
import com.criel.train.business.enumeration.ConfirmOrderStatusEnum;
import com.criel.train.business.enumeration.RedisKeyPreEnum;
import com.criel.train.business.enumeration.SeatColEnum;
import com.criel.train.business.enumeration.SeatTypeEnum;
import com.criel.train.business.req.ConfirmOrderTicketReq;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.mapper.ConfirmOrderMapper;
import com.criel.train.business.req.ConfirmOrderQueryReq;
import com.criel.train.business.req.ConfirmOrderSaveReq;
import com.criel.train.business.resp.ConfirmOrderQueryResp;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    private static final List<String> ydzReferSeatList = List.of("A1", "C1", "D1", "F1", "A2", "C2", "D2", "F2");
    private static final List<String> edzReferSeatList = List.of("A1", "B1", "C1", "D1", "F1", "A2", "B2", "C2", "D2", "F2");

    @Autowired
    private ConfirmOrderMapper confirmOrderMapper;

    @Autowired
    private DailyTrainTicketService dailyTrainTicketService;

    @Autowired
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Autowired
    private DailyTrainSeatService dailyTrainSeatService;

    @Autowired
    private AfterConfirmOrderService afterConfirmOrderService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SkTokenService skTokenService;

    public void save(ConfirmOrderSaveReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowflakeUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        if (req.getTrainCode() != null && !req.getTrainCode().isEmpty()) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }
        if (req.getDate() != null) {
            criteria.andDateEqualTo(req.getDate());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 处理订单
     *
     * @param dto
     */
    @SentinelResource(value = "confirm", blockHandler = "confirmBlockHandler")
    public void confirm(ConfirmOrderMQDto dto) {
        String trainCode = dto.getTrainCode();
        Date date = dto.getDate();

        // 分布式锁
        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER.getCode() + trainCode + ":" + DateUtil.formatDate(date);
        RLock rLock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 自带看门狗机制，参数是（最大等待时间，单位）
            locked = rLock.tryLock(10, TimeUnit.SECONDS);
            if (!locked) {
                // 在MQ消费中被调用，这里就无需抛异常
                return;
            }

            // 获得锁成功后做循坏执行购票逻辑
            while (true) {
                // 获取订单date日期trainCode车次的所有的状态为INIT的订单信息
                ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
                confirmOrderExample.createCriteria()
                        .andDateEqualTo(date)
                        .andTrainCodeEqualTo(trainCode)
                        .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());

                // 每次循环最多查询5条数据
                PageHelper.startPage(1, 5);
                List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExampleWithBLOBs(confirmOrderExample);

                // 数据全部处理完则结束循环
                if (confirmOrderList == null || confirmOrderList.isEmpty()) {
                    break;
                }

                // 处理查询到的订单的购票逻辑
                confirmOrderList.forEach(confirmOrder -> {
                    try {
                        ticketHandler(confirmOrder);
                    } catch (BusinessException e) {
                        if (e.getAnEnum() == BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR) {
                            LOG.info("处理订单{}时余票不足", confirmOrder.getId());
                            // 更新订单状态，更新confirm_order订单表状态为：EMPTY无票
                            confirmOrder.setStatus(ConfirmOrderStatusEnum.EMPTY.getCode());
                            updateConfirmOrderStatus(confirmOrder);
                        } else {
                            throw e;
                        }
                    }
                });
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 确保锁被当前线程持有时才释放
            if (locked && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }

    /**
     * 处理一个订单的购票逻辑
     *
     * @param confirmOrder
     */
    private void ticketHandler(ConfirmOrder confirmOrder) {

        // 更新订单状态，更新confirm_order订单表状态为：PENDING处理中
        confirmOrder.setStatus(ConfirmOrderStatusEnum.PENDING.getCode());
        updateConfirmOrderStatus(confirmOrder);

        // 多次使用的数据
        Date date = confirmOrder.getDate();
        String trainCode = confirmOrder.getTrainCode();
        String start = confirmOrder.getStart();
        String end = confirmOrder.getEnd();
        List<ConfirmOrderTicketReq> tickets = JSON.parseArray(confirmOrder.getTickets(), ConfirmOrderTicketReq.class);
        int ticketCount = tickets.size();
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        Integer startIndex = dailyTrainTicket.getStartIndex();
        Integer endIndex = dailyTrainTicket.getEndIndex();

        // 通过查询余票的dailyTrainTicket，计算是否足够，不足则抛异常
        for (ConfirmOrderTicketReq confirmOrderTicketReq : tickets) {
            SeatTypeEnum seatTypeEnum = SeatTypeEnum.getEnumByCode(confirmOrderTicketReq.getSeatTypeCode());
            switch (Objects.requireNonNull(seatTypeEnum)) {
                case YDZ -> {
                    int ydzCount = dailyTrainTicket.getYdz() - 1;
                    if (ydzCount < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(ydzCount);
                }
                case EDZ -> {
                    int edzCount = dailyTrainTicket.getEdz() - 1;
                    if (edzCount < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(edzCount);
                }
                case RW -> {
                    int rwCount = dailyTrainTicket.getRw() - 1;
                    if (rwCount < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(rwCount);
                }
                case YW -> {
                    int ywCount = dailyTrainTicket.getYw() - 1;
                    if (ywCount < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(ywCount);
                }
            }
        }

        // 上面没有异常则开始选座
        // 获取第1张票，用于获取每个ticket中相同的数据：选座信息、座位类型等
        ConfirmOrderTicketReq ticketFirst = tickets.get(0);
        // 获取该车次所有车厢
        List<DailyTrainCarriage> carriages = dailyTrainCarriageService.selectBySeatTypeAndDateAndTrainCode(
                ticketFirst.getSeatTypeCode(),
                date,
                trainCode);

        // 选座结果保存到seatsResult
        List<DailyTrainSeat> seatsResult = new ArrayList<>();
        // 选择座位位置
        if (ticketFirst.getSeat() == null || ticketFirst.getSeat().isEmpty()) {
            // 用户无选座
            LOG.info("用户无选座");

            // 顺序查找座位
            tryFindSeats(
                    carriages, seatsResult,
                    date, trainCode, ticketCount,
                    startIndex, endIndex);
        } else {
            // 用户有选座 (有选座时，每个乘车人的座位类型都一样)
            LOG.info("用户有选座");

            List<SeatColEnum> cols = SeatColEnum.getColsByType(ticketFirst.getSeatTypeCode());
            int colCount = cols.size();
            // 参考列表
            List<String> referSeatList = ydzReferSeatList;
            if (colCount == 5) {
                referSeatList = edzReferSeatList;
            }
            // 获取每个座位的偏移值
            // 遍历到第row行：第i个座位在为seats中的索引为：colCount * row + seatOffsets[i]
            List<Integer> seatOffsets = new ArrayList<>();
            for (ConfirmOrderTicketReq ticket : tickets) {
                seatOffsets.add(referSeatList.indexOf(ticket.getSeat()));
            }

            // 查找该车次中是否有符合【位置条件】的座位
            boolean isFound = tryFindSeatsByCondition(
                    carriages, seatsResult,
                    date, trainCode, seatOffsets,
                    colCount, ticketCount,
                    startIndex, endIndex);
            // 判断是否成功选上座位
            if (!isFound) {
                // 顺序查找座位
                tryFindSeats(
                        carriages, seatsResult,
                        date, trainCode, ticketCount,
                        startIndex, endIndex);
            }

        }

        // 更新seatsResult中每一个seat的sell字段
        for (DailyTrainSeat seat : seatsResult) {
            setSeatSell(seat, startIndex, endIndex);
        }

        // 保存最终选票结果（事务）
        if (!seatsResult.isEmpty()) {
            try {
                afterConfirmOrderService.afterConfirm(dailyTrainTicket, seatsResult, tickets, confirmOrder);
            } catch (Exception e) {
                LOG.error("保存最终选票结果时，发生异常");
                throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SAVE_ERROR);
            }
        } else {
            LOG.error("保存最终选票结果时，seatsResult为null或者为空");
            // TODO 应该需要做结果处理，抛出异常什么的
        }
    }

    /**
     * 更新订单状态到数据库
     *
     * @param confirmOrder 订单数据，包括新状态
     */
    private void updateConfirmOrderStatus(ConfirmOrder confirmOrder) {
        ConfirmOrder newConfirmOrder = new ConfirmOrder();
        newConfirmOrder.setId(confirmOrder.getId());
        newConfirmOrder.setStatus(confirmOrder.getStatus());
        newConfirmOrder.setUpdateTime(new Date());
        confirmOrderMapper.updateByPrimaryKeySelective(newConfirmOrder);
    }


    /**
     * 查找该车次中是否有符合【位置条件】的座位
     * 如果找到，则把座位信息添加到seatsResult中
     *
     * @param carriages
     * @param seatsResult 座位结果集，找到座位后，把座位信息添加到该列表中
     * @param date
     * @param trainCode
     * @param seatOffsets
     * @param colCount    列数，遍历时使用，调用时已经确定是几等座，该值也确定了
     * @param ticketCount
     * @param startIndex
     * @param endIndex
     * @return
     */
    private boolean tryFindSeatsByCondition(List<DailyTrainCarriage> carriages,
                                            List<DailyTrainSeat> seatsResult,
                                            Date date, String trainCode, List<Integer> seatOffsets,
                                            int colCount, int ticketCount,
                                            Integer startIndex, Integer endIndex) {
        // 遍历车厢
        for (DailyTrainCarriage carriage : carriages) {
            List<DailyTrainSeat> seats = dailyTrainSeatService.selectByCarriageIndexAndDateAndTrainCode(carriage.getIndex(), date, trainCode);
            int seatCount = seats.size();
            // 数据库中seat的索引从1开始，但这里在seats列表中，就是直接从0开始
            // 遍历当前车厢内的符合【位置条件】的座位，不一定满足【售卖条件】
            for (int row = 0; row < carriage.getRowCount(); row++) {
                // 遍历每个选定的座位位置
                int checkedCount = 0;
                for (Integer seatOffset : seatOffsets) {
                    int seatIndex = colCount * row + seatOffset;
                    // 检查溢出
                    if (seatIndex >= seatCount) {
                        break;
                    }
                    DailyTrainSeat seat = seats.get(seatIndex);
                    // 检查【售卖条件】
                    if (checkSeat(seat, startIndex, endIndex)) {
                        checkedCount++;
                    }
                }

                // 若座位都选上了，再一次性处理多张票的数据
                if (checkedCount == ticketCount) {
                    for (Integer seatOffset : seatOffsets) {
                        DailyTrainSeat seat = seats.get(colCount * row + seatOffset);
                        // 保存数据到seatsResult
                        seatsResult.add(seat);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 顺序查找座位
     * 由于调用该方法前已经校验过余票是否充足，所以查找结果一定为ture（应该）
     * (不过多线程下可能会有问题，后面再说)
     *
     * @param carriages
     * @param seatsResult
     * @param date
     * @param trainCode
     * @param ticketCount
     * @param startIndex
     * @param endIndex
     */
    private void tryFindSeats(List<DailyTrainCarriage> carriages,
                              List<DailyTrainSeat> seatsResult,
                              Date date, String trainCode, int ticketCount,
                              Integer startIndex, Integer endIndex) {
        int checkedCount = 0;
        // 遍历车厢
        for (DailyTrainCarriage carriage : carriages) {
            List<DailyTrainSeat> seats = dailyTrainSeatService.selectByCarriageIndexAndDateAndTrainCode(carriage.getIndex(), date, trainCode);
            // 遍历座位
            for (DailyTrainSeat seat : seats) {
                // 检查【售卖条件】
                if (checkSeat(seat, startIndex, endIndex)) {
                    checkedCount++;
                    // 保存数据到seatsResult
                    seatsResult.add(seat);
                    if (checkedCount == ticketCount) {
                        return;
                    }
                }
            }
        }
        LOG.error("顺序查找座位时，并没有找到合适的座位");
    }

    /**
     * 根据座位的sell值，检查该座位，在对应区间是否可卖
     * sell的值：例如有6个站，sell = "11001"，表示每个站站区间内的票是否已卖出
     * 站序从1开始，若购买的票是第1站到第4站，则需要”*000*“
     *
     * @param seat
     * @param startIndex
     * @param endIndex
     * @return
     */
    private boolean checkSeat(DailyTrainSeat seat, int startIndex, int endIndex) {
        // 序号需要改为从0开始
        String sell = seat.getSell();
        String sellPart = sell.substring(startIndex - 1, endIndex - 1);
        // 返回seat这个座位是否可选
        return !sellPart.contains("1");
    }

    /**
     * 设置座位的sell值，将区间内变为1
     *
     * @param seat
     * @param startIndex
     * @param endIndex
     */
    private void setSeatSell(DailyTrainSeat seat, int startIndex, int endIndex) {
        StringBuilder resSell = new StringBuilder(seat.getSell());
        for (int i = startIndex - 1; i < endIndex - 1; i++) {
            resSell.setCharAt(i, '1');
        }
        seat.setSell(resSell.toString());
    }

    /**
     * 降级方法
     *
     * @param req 原方法参数
     * @param e
     */
    private void confirmBlockHandler(ConfirmOrderSaveReq req, BlockException e) {
        LOG.info("请求被限流:{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
