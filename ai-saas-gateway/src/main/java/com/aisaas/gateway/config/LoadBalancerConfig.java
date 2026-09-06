package com.aisaas.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.*;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;

/**
 * 负载均衡配置
 */
@Slf4j
@Configuration
public class LoadBalancerConfig {

    /**
     * 自定义负载均衡策略（基于Nacos权重）
     */
    @Bean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        
        return new NacosWeightedLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name
        );
    }

    /**
     * Nacos权重负载均衡器
     */
    public static class NacosWeightedLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {

        private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
        private final String serviceId;
        private final Random random;

        public NacosWeightedLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                         String serviceId) {
            this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
            this.serviceId = serviceId;
            this.random = new Random();
        }

        @Override
        public Mono<Response<ServiceInstance>> choose(Request request) {
            ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable();
            return supplier.get(request).next().map(instances -> processInstanceResponse(instances));
        }

        private Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> instances) {
            if (instances.isEmpty()) {
                log.warn("No instances available for service: {}", serviceId);
                return new EmptyResponse();
            }

            // 按权重选择实例
            int totalWeight = 0;
            for (ServiceInstance instance : instances) {
                String weightStr = instance.getMetadata().get("nacos.weight");
                int weight = weightStr != null ? Integer.parseInt(weightStr) : 100;
                totalWeight += weight;
            }

            int randomWeight = random.nextInt(totalWeight);
            int currentWeight = 0;

            for (ServiceInstance instance : instances) {
                String weightStr = instance.getMetadata().get("nacos.weight");
                int weight = weightStr != null ? Integer.parseInt(weightStr) : 100;
                currentWeight += weight;

                if (randomWeight < currentWeight) {
                    log.debug("Selected instance {} for service {}", instance.getHost(), serviceId);
                    return new DefaultResponse(instance);
                }
            }

            // 兜底选择第一个
            return new DefaultResponse(instances.get(0));
        }

        @Override
        public Mono<Response<ServiceInstance>> choose() {
            return choose(null);
        }
    }

    /**
     * 轮询负载均衡策略
     */
    @Bean
    public ReactorLoadBalancer<ServiceInstance> roundRobinLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoundRobinLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name
        );
    }

    /**
     * 随机负载均衡策略
     */
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name
        );
    }
}
