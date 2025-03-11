package com.criel.train.member.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.member.config.MemberApplication;
import com.criel.train.member.domain.Member;
import com.criel.train.member.domain.MemberExample;
import com.criel.train.member.mapper.MemberMapper;
import com.criel.train.member.req.MemberGetCodeReq;
import com.criel.train.member.req.MemberRegisterReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    private static final Logger LOG = LoggerFactory.getLogger(MemberApplication.class);

    @Autowired
    private MemberMapper memberMapper;

    public long count() {
        return memberMapper.countByExample(null);
    }

    /**
     * TODO 会员注册功能，暂未完善，供测试
     *
     * @param req 用户注册请求参数
     * @return 用户id
     */
    public long register(MemberRegisterReq req) {
        // 查询手机号是否已经存在
        String mobile = req.getMobile();
        List<Member> memberList = selectByMobile(mobile);
        if (CollUtil.isNotEmpty(memberList)) {
            throw new BusinessException(BusinessExceptionEnum.MOBILE_IS_EXIST);
        }

        // 创建新member
        Member member = createNewMember(mobile);
        return member.getId();
    }

    /**
     * 返回验证码给前端(?)
     * 这里只是形式...实际上就是以4321为验证码...
     * @param req 前端的发送验证码请求
     * @return
     */
    public void getCode(MemberGetCodeReq req) {
        String mobile = req.getMobile();
        List<Member> memberList = selectByMobile(mobile);
        // 手机号不存在，则注册新用户
        if (CollUtil.isEmpty(memberList)) {
            LOG.info("手机号不存在，注册新用户");
            // 创建新member
            createNewMember(mobile);
        } else {
            LOG.info("手机号已存在，不注册新用户");
        }

        // 生成验证码
//        String code = RandomUtil.randomString(4);
        String code = "4321";
        LOG.info("生成的验证码为:{}", code);

        // TODO 保存短信记录表:手机号，短信验证码，有效期，是否已使用，业务类型，发送时间，使用时间
        // TODO 对接短信通道，发送短信
    }

    /**
     * 创建新用户
     *
     * @param mobile
     * @return
     */
    private Member createNewMember(String mobile) {
        Member member = new Member();
        member.setId(SnowflakeUtil.getSnowflakeNextId());
        member.setMobile(mobile);
        memberMapper.insert(member);
        return member;
    }

    /**
     * 根据手机号查询用户
     *
     * @param mobile
     * @return
     */
    private List<Member> selectByMobile(String mobile) {
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        return memberMapper.selectByExample(memberExample);
    }
}
