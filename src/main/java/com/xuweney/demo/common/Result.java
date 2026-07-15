package com.xuweney.demo.common;

public class Result<T> {
    private int code;
    private String mes;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.mes = "成功";
        r.data = data;
        return r;
    }

    public static Result<?> error(int code, String mes) {
        Result<?> r = new Result<>();
        r.code = code;
        r.mes = mes;
        return r;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMes() { return mes; }
    public void setMes(String mes) { this.mes = mes; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}