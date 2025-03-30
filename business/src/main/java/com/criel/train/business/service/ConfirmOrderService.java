package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.criel.train.business.domain.generated.*;
import com.criel.train.business.enumeration.ConfirmOrderStatusEnum;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    public void confirm(ConfirmOrderSaveReq req) {
        // TODO 数据校验：车次是否存在、余票是否存在、tickets是否为空、乘车人是否已买过相同数据

        // 多次使用的数据
        Date now = new Date();
        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        String start = req.getStart();
        String end = req.getEnd();
        List<ConfirmOrderTicketReq> tickets = req.getTickets();
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        Integer startIndex = dailyTrainTicket.getStartIndex();
        Integer endIndex = dailyTrainTicket.getEndIndex();

        // 保存confirm_order表，初始化状态
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(SnowflakeUtil.getSnowflakeNextId());
        confirmOrder.setMemberId(LoginMemberContext.getId());
        confirmOrder.setDate(date);
        confirmOrder.setTrainCode(trainCode);
        confirmOrder.setStart(start);
        confirmOrder.setEnd(end);
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setTickets(JSON.toJSONString(tickets));
        confirmOrderMapper.insert(confirmOrder);

        // 查询余票，计算是否足够，不足则抛异常
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

        // 选择座位位置
        ConfirmOrderTicketReq ticketFirst = tickets.get(0);
        if (ticketFirst.getSeat() == null || ticketFirst.getSeat().isEmpty()) {
            // 用户无选座
            LOG.info("用户无选座");
            ...

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

            // 获取车厢并遍历
            List<DailyTrainCarriage> carriages = dailyTrainCarriageService.selectBySeatTypeAndDateAndTrainCode(
                    ticketFirst.getSeatTypeCode(),
                    date,
                    trainCode);
            // 查找该车次中是否有符合【位置条件】的座位
            // TODO 方法里找到后修改了seats的值，但是并没有返回数据，如修改，后面保存应该要用到，看看怎么弄
            boolean isFound = tryFindAndSetSeat(carriages, date, trainCode, seatOffsets, colCount, startIndex, endIndex);
            // 判断是否成功选上座位
            if (!isFound) {
                // TODO 执行和用户未选座一样的选座方法（还没写这个方法）
                ...
            }


        }

        // 更新daily_train_ticket每日余票表、daily_train_seat每日座位表

        // 更新购票记录表 (?)

        // 更新confirm_order表状态

    }


    /**
     * 查找该车次中是否有符合【位置条件】的座位
     *
     * @param carriages
     * @param date
     * @param trainCode
     * @param seatOffsets
     * @param colCount
     * @param startIndex
     * @param endIndex
     * @return
     */
    private boolean tryFindAndSetSeat(List<DailyTrainCarriage> carriages, Date date, String trainCode, List<Integer> seatOffsets, int colCount, Integer startIndex, Integer endIndex) {
        boolean isFound = false;
        for (DailyTrainCarriage carriage : carriages) {
            List<DailyTrainSeat> seats = dailyTrainSeatService.selectByCarriageIndexAndDateAndTrainCode(carriage.getIndex(), date, trainCode);
            // 数据库中seat的索引从1开始，但这里在seats列表中，就是直接从0开始
            // 遍历当前车厢内的符合【位置条件】的座位，不一定满足【售卖条件】
            for (int row = 0; row < carriage.getRowCount(); row++) {
                // 遍历每个选定的座位位置
                int checkedCount = 0;
                for (Integer seatOffset : seatOffsets) {
                    DailyTrainSeat seat = seats.get(colCount * row + seatOffset);
                    // 检查【售卖条件】
                    if (checkSeat(seat, startIndex, endIndex)) {
                        checkedCount++;
                    }
                }
                // 若座位都选上了，再修改座位的sell数据
                if (checkedCount == seatOffsets.size()) {
                    for (Integer seatOffset : seatOffsets) {
                        DailyTrainSeat seat = seats.get(colCount * row + seatOffset);
                        // 把sell对应位置变为1111
                        setSeatSell(seat, startIndex, endIndex);
                    }
                    isFound = true;
                    break;
                }
            }
            if (isFound) {
                break;
            }
        }
        return isFound;
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
}
