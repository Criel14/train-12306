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
import com.criel.train.member.req.MemberLoginReq;
import com.criel.train.member.req.MemberRegisterReq;
import com.criel.train.member.resp.MemberLoginResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
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
     * TODO 会员注册功能，供测试
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
    public String getCode(MemberGetCodeReq req) {
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

        return code;
    }

    /**
     * 登录
     * @param req
     * @return
     */
    public MemberLoginResp login(MemberLoginReq req) {
        String mobile = req.getMobile();
        String code = req.getCode();
        List<Member> memberList = selectByMobile(mobile);

        // 如果login请求时手机号不存在，则说明用户没点“获取验证码”，因为获取的时候会自动生成用户
        if (CollUtil.isEmpty(memberList)) {
            // 提示“请先获取验证码”
            throw new BusinessException(BusinessExceptionEnum.GET_CODE_FIRST);
        }

        // 判断验证码是否为空
        if (code == null || code.isEmpty()) {
            throw new BusinessException(BusinessExceptionEnum.CODE_IS_EMPTY);
        }
        // TODO 查redis，判断验证码是否正确/过期等
        // 这里先这样写
        if (!code.equals("4321")) {
            throw new BusinessException(BusinessExceptionEnum.CODE_IS_ERROR);
        }

        MemberLoginResp memberLoginResp = new MemberLoginResp();
        BeanUtils.copyProperties(memberList.get(0), memberLoginResp);
        return memberLoginResp;
    }



    /**
     * 创建新用户
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
