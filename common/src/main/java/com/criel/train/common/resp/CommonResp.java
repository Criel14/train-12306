package com.criel.train.common.resp;

import lombok.Data;

/**
 * 通用返回对象，就是Result类，教程里叫做CommonResp
 * 教程里是在业务里new，然后set值；我直接写上3个静态方法
 * @param <T>
 */
@Data
public class CommonResp<T> {

    /**
     * 业务上的成功或失败
     */
    private boolean success;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 返回泛型数据，自定义类型
     */
    private T content;

    public CommonResp() {
    }

    public CommonResp(T content) {
        this.content = content;
    }

    // 下面几个是自己写的

    public static <T> CommonResp<T> success() {
        CommonResp<T> resp = new CommonResp<>();
        resp.setSuccess(true);
        return resp;
    }

    public static <T> CommonResp<T> success(T content) {
        CommonResp<T> resp = new CommonResp<>();
        resp.setSuccess(true);
        resp.setContent(content);
        return resp;
    }

    public static <T> CommonResp<T> error(String message) {
        CommonResp<T> resp = new CommonResp<>();
        resp.setSuccess(false);
        resp.setMessage(message);
        return resp;
    }
}
