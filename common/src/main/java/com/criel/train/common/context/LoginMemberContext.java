package com.criel.train.common.context;

import com.criel.train.common.resp.MemberLoginResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 线程上下文
 * 主要用来获取ThreadLocal中的member信息
 */
public class LoginMemberContext {
    private static final Logger LOG = LoggerFactory.getLogger(LoginMemberContext.class);

    private static ThreadLocal<MemberLoginResp> member = new ThreadLocal<>();

    public static MemberLoginResp getMember() {
        return member.get();
    }

    public static void setMember(MemberLoginResp member) {
        LoginMemberContext.member.set(member);
    }

    public static Long getId() {
        try {
            return member.get().getId();
        } catch (Exception e) {
            LOG.error("获取登录会员信息异常", e);
            throw e;
        }
    }

}

