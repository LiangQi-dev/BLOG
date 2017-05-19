package com.kcrpc.protocol;

/**
 *
 *  RPC 响应错误结构
 *
 * @author LiangQi.dev@gmail.com
 */
public class RpcError {
    private int code;
    private String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
