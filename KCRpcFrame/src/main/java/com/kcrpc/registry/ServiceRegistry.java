package com.kcrpc.registry;

/**
 * ZooKeeper 服务注册接口
 *
 * @author LiangQi.dev@gmail.com
 */
public interface ServiceRegistry {
    /**
     * 注册服务信息
     *
     * @param serviceName   服务名称
     * @param ServiceAddress    服务地址
     */
    void register(String serviceName, String ServiceAddress);
}
