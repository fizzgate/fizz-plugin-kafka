package we.plugin.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Component;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import we.plugin.auth.ApiConfig;

import javax.annotation.PreDestroy;
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

    public FizzKafkaProducer get(ApiConfig apiConfig, Map<String, Object> config) {
        return kafkaProducerHolder.get(apiConfig, config);
    }

    private class KafkaProducerHolder {
        volatile Map<Object, FizzKafkaProducer> kafkaProducerMap = new ConcurrentHashMap<>();

        private KafkaProducerHolder() {
        }

        public FizzKafkaProducer get(ApiConfig apiConfig, Map<String, Object> config) {
            FizzKafkaProducer fizzKafkaProducer = kafkaProducerMap.get(apiConfig.id);
            if (fizzKafkaProducer == null || fizzKafkaProducer.isSenderUpdateRequired(config)) {
                Map<String, Object> producterConfig = mergeConcurrentHashMap(config, kafkaProperties.buildProducerProperties());
                forceInitImportantProperties(apiConfig, producterConfig);
                KafkaSender<Long, String> kafkaSender = buildFizzKafkaSender(producterConfig);
                fizzKafkaProducer = new FizzKafkaProducer(kafkaSender, config);
                kafkaProducerMap.put(apiConfig.id, fizzKafkaProducer);
            }
            return fizzKafkaProducer;
        }

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
