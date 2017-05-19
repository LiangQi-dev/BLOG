package com.kcrpc.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 *  读取application.properties中的配置
 *
 * @author LiangQi.dev@gmail.com
 */
@Configuration
@ConfigurationProperties(prefix = "registry")
public class RegistryConfig {
    private String servers;

    @Bean
    public ServiceRegistry serviceRegister(){
        return new ServiceRegistryImpl(servers);
    }

    public void  setServers(String servers){
        this.servers = servers;
    }
}
