package com.ticket.ddd.controller.model.enums;

import com.ibm.icu.util.LocaleMatcher.Result;
import com.ticket.ddd.controller.model.vo.ResultMessage;

public class ResultUtil<T> {
    private final ResultMessage<T> responseMessage;
    private static final Integer SUCCESS_CODE = 200;
    public ResultUtil() {
        responseMessage = new ResultMessage<>();
        responseMessage.setSuccess(true);
        responseMessage.setMessage("success");
        responseMessage.setCode(SUCCESS_CODE);
    }
    public ResultMessage<T> setData(T t ) {
        responseMessage.setResult(t);
        return responseMessage;
    }

    public ResultMessage<T> setSuccessMsg(ResultCode resultCode) {
        responseMessage.setSuccess(true);
        responseMessage.setMessage(resultCode.message());
        responseMessage.setCode(resultCode.code());
        return responseMessage;
    }
    public static <T> ResultMessage<T> data(T t) {
        return new ResultUtil<T>().setData(t);
    }
    public static <T> ResultMessage<T> success(ResultCode responseStatusCode) {
        return new ResultUtil<T>().setSuccessMsg(responseStatusCode);
    }
    public static <T> ResultMessage<T> success() {
        return new ResultUtil<T>().setSuccessMsg(ResultCode.SUCCESS);
    }
    public static <T> ResultMessage<T> error(ResultCode res) {
        return new ResultUtil<T>().setErrorMsg(res);
    }
    public static <T> ResultMessage<T> error(Integer code,String mes) {
        return new ResultUtil<T>().setErrorMsg(code, mes);
    }
    public ResultMessage<T> setErrorMsg(ResultCode resultCode) {
        responseMessage.setSuccess(false);
        responseMessage.setMessage(resultCode.message());
        responseMessage.setCode(resultCode.code());
        return responseMessage;
    }
    public ResultMessage<T> setErrorMsg(Integer code,String message) {
        responseMessage.setSuccess(false);
        responseMessage.setMessage(message);
        responseMessage.setCode(code);
        return responseMessage;
    }
}
