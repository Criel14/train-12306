package com.criel.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.member.domain.generated.Passenger;
import com.criel.train.member.domain.generated.PassengerExample;
import com.criel.train.member.mapper.PassengerMapper;
import com.criel.train.member.req.PassengerQueryReq;
import com.criel.train.member.req.PassengerSaveReq;
import com.criel.train.member.resp.PassengerQueryResp;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

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
        // 更新id、创建时间字段
        Date currentDate = new Date();
        passenger.setId(SnowflakeUtil.getSnowflakeNextId());
        passenger.setMemberId(LoginMemberContext.getId());
        passenger.setCreateTime(currentDate);
        passenger.setUpdateTime(currentDate);
        passengerMapper.insert(passenger);
    }

    /**
     * 乘客分页查询
     * @return
     */
    public List<PassengerQueryResp> queryList(PassengerQueryReq passengerQueryReq) {
        PassengerExample passengerExample = new PassengerExample();
        PassengerExample.Criteria criteria = passengerExample.createCriteria();
        // 根据memberId查找
        // 如果是管理员调用这里，就可能没有memberId
        if (passengerQueryReq.getMemberId() != null) {
            criteria.andMemberIdEqualTo(passengerQueryReq.getMemberId());
        }
        // 分页查询
        PageHelper.startPage(passengerQueryReq.getPage(), passengerQueryReq.getSize());
        List<Passenger> passengerList = passengerMapper.selectByExample(passengerExample);
        List<PassengerQueryResp> passengerQueryRespList = BeanUtil.copyToList(passengerList, PassengerQueryResp.class);
        return passengerQueryRespList;
    }
}
