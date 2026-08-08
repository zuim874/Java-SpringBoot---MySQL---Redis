package com.xuwenye.demo.common;

/**
 * 统一 API 返回结果封装
 * 1.code：状态码（200 成功 / 400 参数错误 / 401 未认证 / 403 无权限 / 429 限流 / 500 服务器异常）
 * 2.mes：提示信息
 * 3.data：业务数据
 * <p>
 * @author ZuiM
 * @param <T> 业务数据类型
 */
public class Result<T> {
    private int code;
    private String mes;
    private T data;

    /**
     * 成功返回（code=200）
     * <p>
     * @author ZuiM
     * @param data 业务数据
     * @param <T> 数据类型
     * @return Result 成功结果
     */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.mes = "成功";
        r.data = data;
        return r;
    }

    /**
     * 失败返回（自定义状态码与提示）
     * <p>
     * @author ZuiM
     * @param code 状态码（400/401/403/500 等）
     * @param mes 错误提示信息
     * @return Result 失败结果
     */
    public static Result<?> error(int code, String mes) {
        Result<?> r = new Result<>();
        r.code = code;
        r.mes = mes;
        return r;
    }

    /**
     * 获取状态码
     * <p>
     * @author ZuiM
     * @return int 状态码
     */
    public int getCode() { return code; }

    /**
     * 设置状态码
     * <p>
     * @author ZuiM
     * @param code 状态码
     */
    public void setCode(int code) { this.code = code; }

    /**
     * 获取提示信息
     * <p>
     * @author ZuiM
     * @return String 提示信息
     */
    public String getMes() { return mes; }

    /**
     * 设置提示信息
     * <p>
     * @author ZuiM
     * @param mes 提示信息
     */
    public void setMes(String mes) { this.mes = mes; }

    /**
     * 获取业务数据
     * <p>
     * @author ZuiM
     * @return T 业务数据
     */
    public T getData() { return data; }

    /**
     * 设置业务数据
     * <p>
     * @author ZuiM
     * @param data 业务数据
     */
    public void setData(T data) { this.data = data; }
}
