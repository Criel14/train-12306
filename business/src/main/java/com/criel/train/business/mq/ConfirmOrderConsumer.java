package com.criel.train.business.mq;

import com.alibaba.fastjson.JSON;
import com.criel.train.business.req.ConfirmOrderQueryReq;
import com.criel.train.business.req.ConfirmOrderSaveReq;
import com.criel.train.business.service.ConfirmOrderService;
import com.criel.train.common.constant.RocketMQTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 处理订单-消费者
 */
@Service
@Slf4j
@RocketMQMessageListener(consumerGroup = "default", topic = RocketMQTopicConstant.CONFIRM_ORDER_TOPIC)
public class ConfirmOrderConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private ConfirmOrderService confirmOrderService;

    @Override
    public void onMessage(MessageExt messageExt) {
        byte[] body = messageExt.getBody();
        log.info("收到消息:{}", new String(body));
        ConfirmOrderSaveReq req = JSON.parseObject(body, ConfirmOrderSaveReq.class);
        confirmOrderService.confirm(req);
    }
}
