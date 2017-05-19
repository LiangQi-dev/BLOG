package com.kcrpc.Server;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.kcrpc.protocol.*;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * RPC 服务端处理器（用于处理 RPC 请求）
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerHandler.class);
    private final Map<String, Object> handlerMap;

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx,final String req) throws Exception {
        LOGGER.info("新请求进入：{}",req);
        RpcServer.submit(new Runnable() {
            @Override
            public void run(){
                RpcRequest request = JSON.parseObject(req,RpcRequest.class);
                LOGGER.info("开始处理请求：{}...",req);
                LOGGER.info("请求方法:{}", request.getMethod() );
                LOGGER.info("请求参数:{}", request.getParams() );

                RpcResponse response = new RpcResponse();
                response.setJsonrpc(request.getJsonrpc());
                response.setId(request.getId());

                try {
                    response.setJsonrpc(request.getJsonrpc());
                    try {
                        Object result = handle(request);
                        response.setResult(result);
                    } catch (NullPointerException e) {
                        RpcError error = new RpcError();
                        error.setCode(ConstRpcCode.PRCMethodNullCode);
                        error.setMessage(ConstRpcMessage.PRCMethodNullMessage);
                        response.setError(error);
                    } catch (NoSuchMethodError e) {
                        RpcError error = new RpcError();
                        error.setCode(ConstRpcCode.NoSuchMethodCode);
                        error.setMessage(ConstRpcMessage.NoSuchMethodMessage);
                        response.setError(error);
                    }
                } catch (JSONException e) {
                    RpcError error = new RpcError();
                    error.setCode(ConstRpcCode.JSONExceptionCode);
                    error.setMessage(ConstRpcMessage.JSONExceptionMessage);
                    response.setError(error);
                }

                final String resp = JSON.toJSONString(response) + "\n";
                ctx.channel().writeAndFlush(resp).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        LOGGER.info("返回响应：{}", resp);
                    }
                });

            }
        });
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        LOGGER.info("新连接进入:{}",ctx.channel().remoteAddress());
        LOGGER.info("新连接Chanel:{}",ctx.channel().id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        LOGGER.info("连接关闭:{}",ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("服务异常:{}", cause);
        ctx.close();
    }

    private Object handle(RpcRequest request){
        if (request.getMethod() == null) {
            throw new NullPointerException();
        }else {
            // 获取服务对象
            String[] serviceInterface = request.getMethod().split("\\.");
            String serviceName = serviceInterface[0];
            Object serviceBean = handlerMap.get(serviceName);
            if (serviceBean == null) {
                throw new RuntimeException(String.format("没有发现服务对象: %s", serviceName));
            }
            // 获取反射调用所需的参数
            Class<?> serviceClass = serviceBean.getClass();
            String methodName = serviceInterface[1];

            Object[] parameters = new Object[]{request.getParams()};
            Class[] clazzs = null;

            if (request.getParams() != null){
                clazzs = new Class[]{String.class};
            }

            // 使用 CGLib 执行反射调用
            FastClass serviceFastClass = FastClass.create(serviceClass);
            Object o = null;
            try {
                FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, clazzs);
                o = serviceFastMethod.invoke(serviceBean, parameters);
            } catch (NoSuchMethodError e) {
                throw e;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }finally {
                LOGGER.info("方法执行完毕，等待放回响应...");
            }
            return o;
        }
    }


}
