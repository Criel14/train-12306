package com.criel.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.member.config.MemberApplication;
import com.criel.train.member.domain.generated.Passenger;
import com.criel.train.member.domain.generated.PassengerExample;
import com.criel.train.member.mapper.PassengerMapper;
import com.criel.train.member.req.PassengerQueryReq;
import com.criel.train.member.req.PassengerSaveReq;
import com.criel.train.member.resp.PassengerQueryResp;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PassengerService {
    private static final Logger LOG = LoggerFactory.getLogger(MemberApplication.class);

    @Autowired
    PassengerMapper passengerMapper;

    /**
     * 保存用户
     *
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
     *
     * @return
     */
    public PageResp<PassengerQueryResp> queryList(PassengerQueryReq passengerQueryReq) {
        PassengerExample passengerExample = new PassengerExample();
        // 指定排序规则：按照 name 字段的中文拼音升序排序
        passengerExample.setOrderByClause("name COLLATE utf8mb4_zh_0900_as_cs ASC");
        PassengerExample.Criteria criteria = passengerExample.createCriteria();

        // 根据memberId查找
        // 如果是管理员调用这里，就可能没有memberId
        if (passengerQueryReq.getMemberId() != null) {
            criteria.andMemberIdEqualTo(passengerQueryReq.getMemberId());
        }

        // 分页查询
        LOG.info("分页查询：查询页码：{}, 每页条数：{}", passengerQueryReq.getPage(), passengerQueryReq.getSize());
        PageHelper.startPage(passengerQueryReq.getPage(), passengerQueryReq.getSize());
        List<Passenger> passengerList = passengerMapper.selectByExample(passengerExample);
        PageInfo<Passenger> pageInfo = new PageInfo<>(passengerList);
        LOG.info("分页查询：总条数：{}, 总页数：{}", pageInfo.getTotal(), pageInfo.getPages());

        // 封装返回结果
        PageResp<PassengerQueryResp> resp = new PageResp<>();
        resp.setTotal(pageInfo.getTotal());
        resp.setList(BeanUtil.copyToList(passengerList, PassengerQueryResp.class));
        return resp;
    }
}
