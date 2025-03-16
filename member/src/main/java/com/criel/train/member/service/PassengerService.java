package com.criel.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.member.domain.generated.Passenger;
import com.criel.train.member.mapper.PassengerMapper;
import com.criel.train.member.req.PassengerSaveReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class PassengerService {

    @Autowired
    PassengerMapper passengerMapper;

    /**
     * 保存用户
     * @param passengerSaveReq
     */
    public void save(PassengerSaveReq passengerSaveReq) {
        Passenger passenger = BeanUtil.copyProperties(passengerSaveReq, Passenger.class);
        passengerMapper.insert(passenger);
        // 更新id、创建时间字段
        // TODO 还需要关联会员，修改memberId字段
        Date currentDate = new Date();
        passenger.setId(SnowflakeUtil.getSnowflakeNextId());
        passenger.setCreateTime(currentDate);
        passenger.setUpdateTime(currentDate);
    }


}
