package com.kcrpc.Server;

import com.kcrpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 *  RPC 服务启动类
 *
 * @author LiangQi.dev@gmail.com
 */
@Component("rpcServer")
public class RpcServer implements ApplicationContextAware,InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    @Autowired
    private ServiceRegistry serviceRegistry;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Map<String, Object> exportServiceMap = new HashMap<>();
    private static ThreadPoolExecutor threadPoolExecutor;

    @Value("${server.port}")
    private int serverPort;
    private String serverAddress;

    /**
     * 获取spring ioc接管的所有bean
     * @param ctx
     * @throws BeansException
     */
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        try {
            serverAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        Map<String, Object> serviceMap = ctx.getBeansWithAnnotation(RpcService.class); // 获取所有带有 RpcService 注解的 Spring Bean
        logger.info("获取到所有的RPC服务:{}", serviceMap);
        for (String serviceName : serviceMap.keySet()){
            Object bean = serviceMap.get(serviceName);

            /**
             * 此处解决标注了@Transactional的方法在使用反射调用时获取到的不是真实对象,而是代理类的问题
             *
             */
            if (AopUtils.isAopProxy(bean)){
                try {
                    if(AopUtils.isJdkDynamicProxy(bean)) {
                        bean = getJdkDynamicProxyTargetObject(bean);
                    } else{ //cglib
                        bean = getCglibProxyTargetObject(bean);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            logger.info("开始注册服务{} 到Zookeeper上...",serviceName);
            exportServiceMap.put(serviceName, bean);
            serviceRegistry.register(serviceName,String.format("tcp@%s:%d",serverAddress,serverPort));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("开始启动 RPC 服务...");
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .option(ChannelOption.TCP_NODELAY,true);
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline()
                            .addLast(new LineBasedFrameDecoder(65536))
                            .addLast(new StringDecoder())
                            .addLast(new StringEncoder())
                            .addLast(new RpcServerHandler(exportServiceMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);


            //开始监听RPC连接
            ChannelFuture future = serverBootstrap.bind(serverAddress,serverPort).sync();

            logger.info("服务启动成功，并开始监听端口{},等待连接... " , serverPort );
            future.channel().closeFuture().sync();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void submit(Runnable task){
        if(threadPoolExecutor == null){
            synchronized (RpcServer.class) {
                if(threadPoolExecutor == null){
                    threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
                }
            }
        }
        threadPoolExecutor.submit(task);
    }

    /**
     * 获取Cglib动态代理的真实对象
     * @param proxy
     * @return
     * @throws Exception
     */
    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        return target;
    }

    /**
     * 获取JDK动态代理的真实对象
     * @param proxy
     * @return
     * @throws Exception
     */
    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();
        return target;
    }
}
