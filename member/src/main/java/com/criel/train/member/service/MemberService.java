package com.criel.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.criel.train.common.constant.BusinessConstant;
import com.criel.train.common.constant.RedisKeyConstant;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import com.criel.train.common.properties.SmsProperties;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.member.config.MemberApplication;
import com.criel.train.member.domain.generated.Member;
import com.criel.train.member.domain.generated.MemberExample;
import com.criel.train.member.domain.SmsRecord;
import com.criel.train.member.mapper.MemberMapper;
import com.criel.train.member.req.MemberGetCodeReq;
import com.criel.train.member.req.MemberLoginReq;
import com.criel.train.member.req.MemberRegisterReq;
import com.criel.train.member.resp.MemberLoginResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MemberService {
    private static final Logger LOG = LoggerFactory.getLogger(MemberApplication.class);

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private SmsProperties smsProperties;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public long count() {
        return memberMapper.countByExample(null);
    }

    /**
     * 会员注册功能（测试用）
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
     * 生成验证码
     * TODO 发送短信
     * 这里只是形式...实际上就是以4321为验证码...
     *
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
        // String code = RandomUtil.randomString(4);
        // 项目中没有真的去发短信，就固定为4321了
        String code = "4321";
        LOG.info("生成的验证码为:{}", code);

        // 保存短信记录：手机号、验证码、有效时间（毫秒）、业务类型、发送时间
        SmsRecord smsRecord = new SmsRecord(
                mobile, code,
                smsProperties.getExpireTime(),
                BusinessConstant.LOGIN,
                LocalDateTime.now());
        this.saveSmsRecord(smsRecord);

        // TODO 对接短信通道，发送短信
    }

    /**
     * 登录
     *
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

        // 校验验证码
        int verifyRes = this.verifyCode(mobile, code);
        LOG.info("验证码校验结果：{}", verifyRes);
        switch (verifyRes) {
            case 0:
                throw new BusinessException(BusinessExceptionEnum.CODE_IS_EXPIRED);
            case -1:
                throw new BusinessException(BusinessExceptionEnum.CODE_IS_ERROR);
            default:
                break;
        }

        // 验证码正确，则返回用户信息
        MemberLoginResp memberLoginResp = BeanUtil.copyProperties(memberList.get(0), MemberLoginResp.class);
        return memberLoginResp;
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

    /**
     * 保存短信记录到redis
     *
     * @param smsRecord
     */
    private void saveSmsRecord(SmsRecord smsRecord) {
        LOG.info("保存短信记录到redis：{}",smsRecord);
        // 定义key
        String key = RedisKeyConstant.SMS_CODE_KEY + smsRecord.getMobile();
        // 保存
        redisTemplate.opsForValue().set(key, smsRecord, smsProperties.getExpireTime(), TimeUnit.MILLISECONDS);
    }

    /**
     * 检验短信验证码
     *
     * @param mobile
     * @param inputCode
     * @return 验证码过期或未获取：0；验证码正确：1；验证码错误 -1
     */
    public int verifyCode(String mobile, String inputCode) {
        String key = RedisKeyConstant.SMS_CODE_KEY + mobile;
        SmsRecord record = (SmsRecord) redisTemplate.opsForValue().get(key);
        if (record == null) {
            return 0; // 没有验证码记录，验证码已过期
        }
        // 校验验证码：是否正确
        if (!record.getCode().equals(inputCode)) {
            return -1;
        }
        // 删除验证码
        LOG.info("删除redis中的验证码信息：{}", key);
        redisTemplate.delete(key);
        return 1;
    }
}
