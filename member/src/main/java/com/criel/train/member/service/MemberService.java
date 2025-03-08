package com.criel.train.member.service;

import com.criel.train.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    @Autowired
    private MemberMapper memberMapper;

    public int testConnect() {
        return memberMapper.testConnect();
    }

}
