package com.criel.train.business.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.criel.train.business.domain.generated.ConfirmOrder;
import com.criel.train.business.dto.ConfirmOrderMQDto;
import com.criel.train.business.enumeration.ConfirmOrderStatusEnum;
import com.criel.train.business.enumeration.RedisKeyPreEnum;
import com.criel.train.business.mapper.ConfirmOrderMapper;
import com.criel.train.business.req.ConfirmOrderSaveReq;
import com.criel.train.common.constant.RocketMQTopicConstant;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import com.criel.train.common.util.SnowflakeUtil;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class BeforeConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

    @Autowired
    private ConfirmOrderMapper confirmOrderMapper;

    @Autowired
    private SkTokenService skTokenService;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 处理订单前置：校验令牌，获取锁后发送消息队列，排队处理后续订单
     *
     * @param req
     */
    @SentinelResource(value = "beforeConfirm", blockHandler = "beforeConfirmBlockHandler")
    public long beforeConfirm(ConfirmOrderSaveReq req) {
        // 校验令牌
        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getId());
        if (!validSkToken) {
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
        }

        // 保存confirm_order订单表，初始化状态
        Date now = new Date();
        long confirmOrderId = SnowflakeUtil.getSnowflakeNextId();
        Long memberId = LoginMemberContext.getId();

        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(confirmOrderId);
        confirmOrder.setMemberId(memberId);
        confirmOrder.setDate(req.getDate());
        confirmOrder.setTrainCode(req.getTrainCode());
        confirmOrder.setStart(req.getStart());
        confirmOrder.setEnd(req.getEnd());
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));
        confirmOrderMapper.insert(confirmOrder);

        // 向MQ发送confirmOrderMQDto
        ConfirmOrderMQDto confirmOrderMQDto = new ConfirmOrderMQDto();
        confirmOrderMQDto.setDate(req.getDate());
        confirmOrderMQDto.setTrainCode(req.getTrainCode());
        confirmOrderMQDto.setLogId(MDC.get("LOG_ID"));

        rocketMQTemplate.convertAndSend(RocketMQTopicConstant.CONFIRM_ORDER_TOPIC, JSON.toJSONString(confirmOrderMQDto));
        LOG.info("排队购票，已发送MQ");

        // 返回订单ID
        return confirmOrderId;
    }

    /**
     * 降级方法
     *
     * @param req 原方法参数
     * @param e
     */
    private void beforeConfirmBlockHandler(ConfirmOrderSaveReq req, BlockException e) {
        LOG.info("请求被限流:{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
