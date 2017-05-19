package com.kcrpc.protocol;

/**
 *
 *  RPC 响应消息数据结构
 *
 * @author LiangQi.dev@gmail.com
 */
public class RpcResponse {
    private int id =0;
    private String jsonrpc;//协议版本号
    private RpcError error;
    private Object result;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public RpcError getError() {
        return error;
    }

    public void setError(RpcError error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "id=" + id +
                ", jsonrpc='" + jsonrpc + '\'' +
                ", error=" + error +
                ", result=" + result +
                '}';
    }
}
