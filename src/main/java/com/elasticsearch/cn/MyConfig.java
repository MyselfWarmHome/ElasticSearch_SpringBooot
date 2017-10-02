package com.elasticsearch.cn;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

/**
 * Created by Panda on 2017/10/2.
 */
@Configuration
@PropertySource(value = "classpath:config/ElasticsearchConfig.properties",ignoreResourceNotFound = true,encoding = "utf-8")
public class MyConfig {

    @Value("${host}")
    private String host;

    @Value("${clusterName}")
    private String cluster_name;


    @Bean
    public TransportClient client() throws Exception{
        //节点的信息
        InetSocketTransportAddress node = new InetSocketTransportAddress(
                InetAddress.getByName(host),
                9300
        );

        //配置类
        Settings settings = Settings.builder()
                .put("cluster.name",cluster_name)
                .build();
        TransportClient client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(node);
        return client;
    }

}
