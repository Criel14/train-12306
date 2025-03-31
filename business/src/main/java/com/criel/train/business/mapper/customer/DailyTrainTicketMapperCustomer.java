package com.criel.train.business.mapper.customer;

import org.apache.ibatis.annotations.Mapper;

import java.util.Date;

@Mapper
public interface DailyTrainTicketMapperCustomer {

    void updateCountBySell(Date date, String trainCode,
                           String seatTypeCode,
                           int minStartIndex, int maxStartIndex,
                           int minEndIndex, int maxEndIndex);
}
