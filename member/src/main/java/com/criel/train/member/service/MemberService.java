package com.criel.train.member.service;

import cn.hutool.core.collection.CollUtil;
import com.criel.train.member.domain.Member;
import com.criel.train.member.domain.MemberExample;
import com.criel.train.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    @Autowired
    private MemberMapper memberMapper;

    public long count() {
        return memberMapper.countByExample(null);
    }

    /**
     * 会员注册功能，暂未完善，供测试
     * @param mobile 手机号
     * @return 用户id
     */
    public long register(String mobile) {
        // 重复检测
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> memberList = memberMapper.selectByExample(memberExample);
        if (CollUtil.isNotEmpty(memberList)) {
            throw new RuntimeException("手机号已被注册");
        }

        // 创建新member
        Member member = new Member();
        // 测试用：暂时用系统时间做id
        member.setId(System.currentTimeMillis());
        member.setMobile(mobile);
        memberMapper.insert(member);
        return member.getId();
    }

}
