package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.criel.train.business.domain.generated.DailyTrainTicket;
import com.criel.train.business.enumeration.ConfirmOrderStatusEnum;
import com.criel.train.business.enumeration.SeatTypeEnum;
import com.criel.train.business.req.ConfirmOrderTicketReq;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.domain.generated.ConfirmOrder;
import com.criel.train.business.domain.generated.ConfirmOrderExample;
import com.criel.train.business.mapper.ConfirmOrderMapper;
import com.criel.train.business.req.ConfirmOrderQueryReq;
import com.criel.train.business.req.ConfirmOrderSaveReq;
import com.criel.train.business.resp.ConfirmOrderQueryResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Autowired
    private ConfirmOrderMapper confirmOrderMapper;

    @Autowired
    private DailyTrainTicketService dailyTrainTicketService;

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

        Date now = new Date();
        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        String start = req.getStart();
        String end = req.getEnd();

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
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));
        confirmOrderMapper.insert(confirmOrder);

        // 查询余票，计算是否足够
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        for (ConfirmOrderTicketReq confirmOrderTicketReq : req.getTickets()) {
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

        // 更新daily_train_ticket每日余票表、daily_train_seat每日座位表

        // 更新购票记录表 (?)

        // 更新confirm_order表状态

    }
}
