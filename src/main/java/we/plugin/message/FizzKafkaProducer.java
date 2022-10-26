

package we.plugin.message;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;


public class FizzKafkaProducer {
    private final KafkaSender<Long, String> sender;
    private long expireStartMills;
    private Map<String, Object> config;
    private static final Logger log = LoggerFactory.getLogger(FizzKafkaProducer.class.getName());

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    public FizzKafkaProducer(KafkaSender<Long, String> sender, Map<String, Object> config) {
        this.sender = sender;
        this.config = config;
        reSetExpireStartMills();
    }

    public boolean isSenderUpdateRequired(Map<String, Object> latest) {
        boolean isExpired = isExpired();
        if (!isExpired) {
            return false;
        } else {
            boolean isConfigChanged = isConfigChanged(latest);
            if (isConfigChanged) {
                return true;
            } else {
                reSetExpireStartMills();
                return false;
            }
        }


    }

    private void reSetExpireStartMills() {
        this.expireStartMills = System.currentTimeMillis();
    }


    private boolean isConfigChanged(Map<String, Object> latest) {
        if (MapUtils.isEmpty(latest)) {
            return false;
        }
        if (latest.size() != config.size()) {
            return true;
        }
        Set<Map.Entry<String, Object>> entrySet = latest.entrySet();
        for (Map.Entry<String, Object> e : entrySet) {
            Object currValue = config.get(e.getKey());
            if (currValue == null) {
                return true;
            }
            if (!StringUtils.equals(currValue.toString(), e.getValue().toString())) {
                return true;
            }
        }
        return false;
    }


    private boolean isExpired() {
        return System.currentTimeMillis() - expireStartMills > 600000;
    }

    /**
     * send a single message
     *
     * @param topic
     * @param message
     * @param response
     * @return
     */
    public Flux<SenderResult<Integer>> sendMessage(String topic, String message) {

        Flux<SenderResult<Integer>> senderResultFlux = sender.send(Flux.range(1, 1).map(i -> SenderRecord.create(new ProducerRecord<>(topic, message), i)))
                .doOnError(e -> {
                    log.error("Send failed", e);
                });

        return senderResultFlux;
    }

    public void close() {
        sender.close();
    }


}