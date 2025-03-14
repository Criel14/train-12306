package com.criel.train.member.resp;

import lombok.Data;

/**
 * （封装返回参数，就是DTO）
 *  用户登录响应
 */
@Data
public class MemberLoginResp {
    private Long id;

    private String mobile;

    private String token;
}
