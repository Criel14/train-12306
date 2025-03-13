package com.criel.train.common.config;

import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.resp.CommonResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 统一异常处理
 * 用@ControllerAdvice注解拦截@Controller注解下的类抛出的异常
 */
@ControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    /**
     * 所有异常统一处理
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public CommonResp exceptionHandler(Exception e) {
        LOG.error("系统异常：", e);
        return CommonResp.error("系统出现异常，请联系管理员");
    }

    /**
     * 业务异常统一处理
     * @param e
     * @return
     */
    @ExceptionHandler(value = BusinessException.class)
    @ResponseBody
    public CommonResp exceptionHandler(BusinessException e) {
        String exceptionDesc = e.getAnEnum().getDesc();
        LOG.error("业务异常：{}", exceptionDesc);
        return CommonResp.error(exceptionDesc);
    }

    /**
     * 校验异常统一处理
     * @param e
     * @return
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public CommonResp exceptionHandler(BindException e) {
        // BindException中可能有多个异常信息，先获取异常信息
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        List<String> exceptionDescs = allErrors.stream().map(ObjectError::getDefaultMessage).toList();
        // 返回异常信息
        LOG.error("校验异常：{}", exceptionDescs);
        // return CommonResp.error(String.join(",", exceptionDescs));
        // 仅返回第一个信息
        return CommonResp.error(exceptionDescs.get(0));
    }

}
