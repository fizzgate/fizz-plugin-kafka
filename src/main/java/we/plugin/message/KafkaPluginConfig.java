package we.plugin.message;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * kafka消息发送服务类
 */
@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaPluginConfig {

    public static final String FIZZ_KAFKA_SENDER_NAME = "fizzKafkaSender";

    // @Bean(name = FIZZ_KAFKA_SENDER_NAME)
    public KafkaSender<Long, String> fizzKafkaSender() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "172.25.33.84:9092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "fizz-message-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<Long, String> senderOptions = SenderOptions.create(props);
        KafkaSender<Long, String> sender = KafkaSender.create(senderOptions);
        return sender;
    }

    @Bean
    public FizzKafkaProducerFactory fizzKafkaProducerFactory(KafkaProperties kafkaProperties) {
        return new FizzKafkaProducerFactory(kafkaProperties);
    }
}
