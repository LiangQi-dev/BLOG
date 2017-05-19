package com.kcrpc.registry;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;


/**
 * ZooKeeper 服务注册实现
 *
 * @author LiangQi.dev@gmail.com
 */
public class ServiceRegistryImpl implements ServiceRegistry,Watcher{
    private static Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryImpl.class);

    private static CountDownLatch latch = new CountDownLatch(1);
    private ZooKeeper zk;

    public ServiceRegistryImpl(String zkService){
        try {
            zk = new ZooKeeper(zkService, RpcConstant.ZK_SESSION_TIMEOUT,this);
            latch.await();
            LOGGER.debug("connected to zookeeper");
        } catch (Exception e) {
            LOGGER.error("create zookeeper client failure",e);
        }
    }

    @Override
    public void register(String serviceName, String serviceAddress) {
        try {
            String registryPath = RpcConstant.ZK_REGISTRY_PATH;
            if (zk.exists(registryPath, false) == null) {
                zk.create(registryPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                LOGGER.info("注册根节点:{}", registryPath);
            }

            String servicePath = registryPath + "/" +serviceName;
            if (zk.exists(servicePath, false) == null) {
                zk.create(servicePath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                LOGGER.info("注册服务节点:{}", servicePath);
            }

            String serviceNode = servicePath + "/" +serviceAddress;
            if (zk.exists(serviceNode, false) == null) {
                zk.create(serviceNode, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                LOGGER.info("注册服务地址节点:{}", serviceNode);
            }

        }catch (Exception e){
            LOGGER.error("注册节点失败",e);
        }
    }


    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getState() == Event.KeeperState.SyncConnected){
            latch.countDown();
        }
    }
}
