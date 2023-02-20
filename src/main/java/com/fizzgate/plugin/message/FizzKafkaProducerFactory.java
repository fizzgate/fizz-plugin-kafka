/*
 *  Copyright (C) 2021 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fizzgate.plugin.message;

import org.apache.commons.collections.CollectionUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import com.fizzgate.plugin.auth.ApiConfig;

import javax.annotation.PreDestroy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取发送kafka消息对象工厂类
 * 合并插件配置和路由配置生成sender对象
 * 每600000秒根据最新配置重新生成sender对象
 */
public class FizzKafkaProducerFactory {
    private final KafkaProperties kafkaProperties;
    private KafkaProducerHolder kafkaProducerHolder = new KafkaProducerHolder();
    
    private static String SERVER_ADDRESSES = "addresses";

    public com.fizzgate.plugin.message.FizzKafkaProducer get(ApiConfig apiConfig, Map<String, Object> config) {
        return kafkaProducerHolder.get(apiConfig, config);
    }

    private class KafkaProducerHolder {
        volatile Map<Object, com.fizzgate.plugin.message.FizzKafkaProducer> kafkaProducerMap = new ConcurrentHashMap<>();

        private KafkaProducerHolder() {
        }

        @SuppressWarnings("unchecked")
		public com.fizzgate.plugin.message.FizzKafkaProducer get(ApiConfig apiConfig, Map<String, Object> config) {
        	Map<String, Object> kfconfig = new HashMap<>();
        	if (config.containsKey(SERVER_ADDRESSES)) {
        		String addrs = (String) config.get(SERVER_ADDRESSES);
        		String[] arr = addrs.split(",");
        		kfconfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Arrays.asList(arr));
        	}
            Map<String, Object> producterConfig = mergeConcurrentHashMap(kfconfig, kafkaProperties.buildProducerProperties());
            List<String> bootstrapServers = (List<String>) producterConfig.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
            String id = genId(bootstrapServers);
            
            com.fizzgate.plugin.message.FizzKafkaProducer fizzKafkaProducer = kafkaProducerMap.get(id);
            if (fizzKafkaProducer == null || fizzKafkaProducer.isSenderUpdateRequired(config)) {
                forceInitImportantProperties(apiConfig, producterConfig);
                KafkaSender<Long, String> kafkaSender = buildFizzKafkaSender(producterConfig);
                fizzKafkaProducer = new com.fizzgate.plugin.message.FizzKafkaProducer(kafkaSender, config);
                kafkaProducerMap.put(id, fizzKafkaProducer);
            }
            return fizzKafkaProducer;
        }

    }
    
    private String genId(List<String> bootstrapServers) {
    	if (CollectionUtils.isNotEmpty(bootstrapServers)) {
    		Collections.sort(bootstrapServers);
    		String s = null;
    		for (String server : bootstrapServers) {
    			if (s == null) {
    				s = server;
    			} else {
    				s = s + "," + server;
    			}
			}
    		return s;
    	}
    	return null;
    }

    private void forceInitImportantProperties(ApiConfig apiConfig, Map<String, Object> producterConfig) {
        if (producterConfig != null) {
            //producterConfig.putIfAbsent("retries", 5);
            producterConfig.putIfAbsent("max.block.ms", 3000l);
            //producterConfig.putIfAbsent("auto.create.topics.enable", true);
            //producterConfig.putIfAbsent("max.in.flight.requests.per.connection", 3);
            producterConfig.putIfAbsent("client.id", apiConfig.service);
        }
    }

    private <K, V> ConcurrentHashMap<K, V> mergeConcurrentHashMap(Map<K, V> from, Map<K, V> to) {
        ConcurrentHashMap result = new ConcurrentHashMap(to);
        for (Map.Entry<? extends K, ? extends V> e : from.entrySet()) {
            if (e.getValue() != null) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }


    public KafkaSender<Long, String> buildFizzKafkaSender(Map<String, Object> props) {
        //    final Map<String, Object> props = kafkaProperties.getProducer().buildProperties();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getProducer());
//        props.put(ProducerConfig.CLIENT_ID_CONFIG, "fizz-message-producer");
//        props.put(ProducerConfig.ACKS_CONFIG, "all");
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<Long, String> senderOptions = SenderOptions.create(props);
        KafkaSender<Long, String> sender = KafkaSender.create(senderOptions);
        return sender;
    }


    public FizzKafkaProducerFactory(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @PreDestroy
    public void closeSenders() {
        if (kafkaProducerHolder.kafkaProducerMap != null && kafkaProducerHolder.kafkaProducerMap.size() > 0) {
            kafkaProducerHolder.kafkaProducerMap.values().stream().forEach(x -> {
                x.close();
            });
        }
    }
}
