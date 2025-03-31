package com.criel.train.business.service;

import com.criel.train.business.domain.generated.*;
import com.criel.train.business.enumeration.ConfirmOrderStatusEnum;
import com.criel.train.business.feign.MemberFeign;
import com.criel.train.business.mapper.ConfirmOrderMapper;
import com.criel.train.business.mapper.customer.DailyTrainTicketMapperCustomer;
import com.criel.train.business.req.ConfirmOrderTicketReq;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.req.MemberTicketReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 在ConfirmOrderService中，计算完选中的座位后，需要做数据库的保存操作
 * 于是，调用这里的事务方法
 * (同一个类中，非事务方法调用事务方法，事务不生效)
 */
@Service
public class AfterConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(AfterConfirmOrderService.class);

    @Autowired
    private ConfirmOrderMapper confirmOrderMapper;

    @Autowired
    private DailyTrainSeatService dailyTrainSeatService;

    @Autowired
    private DailyTrainTicketMapperCustomer dailyTrainTicketMapperCustomer;

    @Autowired
    private MemberFeign memberFeign;

    /**
     * 执行完ConfirmOrderService的confirm方法后，执行该方法
     * 用于保存数据到持久层
     *
     * @param dailyTrainTicket
     * @param seatsResult
     */
    @Transactional
    public void afterConfirm(DailyTrainTicket dailyTrainTicket,
                             List<DailyTrainSeat> seatsResult,
                             List<ConfirmOrderTicketReq> tickets,
                             ConfirmOrder confirmOrder) {

        int startIndex = dailyTrainTicket.getStartIndex();
        int endIndex = dailyTrainTicket.getEndIndex();

        for (int j = 0; j < seatsResult.size(); j++) {
            ConfirmOrderTicketReq ticket = tickets.get(j);
            DailyTrainSeat seat = seatsResult.get(j);

            // 更新daily_train_seat每日座位表的sell字段
            dailyTrainSeatService.updateSellById(seat.getId(), seat.getSell());

            // 更新daily_train_ticket每日余票表

            String sell = seat.getSell();

            // 包含了本次购买区间的余票都要减1，例如5个站，"1000"变成了"1011"，那么有5个区间被影响，只有第2个0这个区间没被影响
            // 注意：数据库中站序从1开始，sell从0开始索引：
            //      则有如下的映射关系：【第i个区间】即sell[i]，表示从【第i+1起点站】到【第i+2到达站】
            // 这些区间的4个边界如下：

            // 区间左端点的站序最小值：sell中从startIndex往前遇到的最后1个'0'的索引，也就是遇到的第1个'1'的下一个索引
            int minStartIndex = 1;
            for (int i = startIndex - 1; i >= 0; i--) {
                if (sell.charAt(i) == '1') {
                    minStartIndex = i + 2; // 第i+1个区间，第i+2起点站
                    break;
                }
            }

            // 区间左端点的站序最大值
            int maxStartIndex = endIndex - 1;

            // 区间右端点的站序最小值
            int minEndIndex = startIndex + 1;

            // 区间右端点的站序最大值：sell中从endIndex往后遇到的最后1个'0'的索引，也就是遇到的第1个'1'的下一个索引
            int maxEndIndex = sell.length() + 1;
            for (int i = endIndex; i < sell.length(); i++) {
                if (sell.charAt(i) == '1') {
                    maxEndIndex = i + 1; // 第i-1个区间，第i+1到达站
                    break;
                }
            }

            // 调用持久层更新daily_train_ticket中的数据
            dailyTrainTicketMapperCustomer.updateCountBySell(
                    seat.getDate(), seat.getTrainCode(), seat.getSeatType(),
                    minStartIndex, maxStartIndex, minEndIndex, maxEndIndex);


            // 更新ticket会员购票记录表
            MemberTicketReq memberTicketReq = createMemberTicketReq(dailyTrainTicket, ticket, seat);
            memberFeign.save(memberTicketReq);

            // 更新confirm_order表状态
            ConfirmOrder newConfirmOrder = new ConfirmOrder();
            newConfirmOrder.setId(confirmOrder.getId());
            newConfirmOrder.setStatus(ConfirmOrderStatusEnum.SUCCESS.getCode());
            newConfirmOrder.setUpdateTime(new Date());
            confirmOrderMapper.updateByPrimaryKeySelective(newConfirmOrder);
        }
    }

    /**
     * 构造保存会员购票信息参数
     *
     * @param dailyTrainTicket
     * @param ticket
     * @param seat
     * @return
     */
    private static MemberTicketReq createMemberTicketReq(DailyTrainTicket dailyTrainTicket, ConfirmOrderTicketReq ticket, DailyTrainSeat seat) {
        MemberTicketReq memberTicketReq = new MemberTicketReq();
        memberTicketReq.setMemberId(LoginMemberContext.getId());
        memberTicketReq.setPassengerId(ticket.getPassengerId());
        memberTicketReq.setPassengerName(ticket.getPassengerName());
        memberTicketReq.setTrainDate(dailyTrainTicket.getDate());
        memberTicketReq.setTrainCode(dailyTrainTicket.getTrainCode());
        memberTicketReq.setCarriageIndex(seat.getCarriageIndex());
        memberTicketReq.setSeatRow(seat.getRow());
        memberTicketReq.setSeatCol(seat.getCol());
        memberTicketReq.setStartStation(dailyTrainTicket.getStart());
        memberTicketReq.setStartTime(dailyTrainTicket.getStartTime());
        memberTicketReq.setEndStation(dailyTrainTicket.getEnd());
        memberTicketReq.setEndTime(dailyTrainTicket.getEndTime());
        memberTicketReq.setSeatType(seat.getSeatType());
        return memberTicketReq;
    }
}
