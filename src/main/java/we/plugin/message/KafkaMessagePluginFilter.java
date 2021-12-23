package we.plugin.message;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;
import we.plugin.FizzPluginFilter;
import we.plugin.FizzPluginFilterChain;
import we.plugin.auth.ApiConfig;
import we.spring.http.server.reactive.ext.FizzServerHttpRequestDecorator;
import we.spring.http.server.reactive.ext.FizzServerHttpResponseDecorator;
import we.util.NettyDataBufferUtils;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Component(KafkaMessagePluginFilter.KAFKA_MESSAGE_PLUGIN) // 必须，且为插件 id
public class KafkaMessagePluginFilter implements FizzPluginFilter {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessagePluginFilter.class);

    public static final String KAFKA_MESSAGE_PLUGIN = "kafkaMessagePlugin"; // 插件 id

    static final String TOPIC_NAME = "fizz-plugin-kafka-topic";

    public static final String HEADER_FIZZ_PLUGIN_KAFKA_METADATA = "fizz-plugin-kafka-metadata";

    public static final String HEADER_FIZZ_PLUGIN_KAFKA_RESULT = "fizz-plugin-kafka-result";


    @Resource(name = "fizzKafkaProducerFactory")
    private FizzKafkaProducerFactory fizzKafkaProducerFactory;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {

        FizzServerHttpRequestDecorator request = (FizzServerHttpRequestDecorator) exchange.getRequest();

        return
                request.getBody().defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                        .single()
                        .flatMap(
                                body -> {
                                    String topic = (String) config.get(TOPIC_NAME);
                                    List<String> topics = request.getHeaders().get(TOPIC_NAME);
                                    if (topics != null && topics.size() > 0) {
                                        topic = topics.get(0);
                                    }
                                    final String sendTopic = topic;
                                    final Message message = new Message();
                                    final String requestBody = body.toString(StandardCharsets.UTF_8); // 请求体对应的字符串
                                    message.getRequest().setBody(requestBody);
                                    message.getRequest().setId(exchange.getRequest().getId());
                                    message.getRequest().setHeaders(exchange.getRequest().getHeaders().toSingleValueMap());
                                    message.getRequest().setCookies(exchange.getRequest().getCookies().toSingleValueMap());
                                    message.getRequest().setInetSocketAddress(exchange.getRequest().getLocalAddress());
                                    message.getRequest().setRemoteAddress(exchange.getRequest().getRemoteAddress());
                                    message.getRequest().setPath(exchange.getRequest().getPath().toString());
                                    message.getRequest().setQueryParams(exchange.getRequest().getQueryParams().toSingleValueMap());
                                    message.getRequest().setMethod(exchange.getRequest().getMethodValue());
                                    ServerHttpResponse original = exchange.getResponse();
                                    FizzServerHttpResponseDecorator fizzServerHttpResponseDecorator = new FizzServerHttpResponseDecorator(original) {
                                        @Override
                                        public Publisher<? extends DataBuffer> writeWith(DataBuffer remoteResponseBody) {
                                            String responseString = remoteResponseBody.toString(StandardCharsets.UTF_8);
                                            message.getResponse().setBody(responseString);
                                            ApiConfig apiConfig = WebUtils.getApiConfig(exchange);
                                            FizzKafkaProducer fizzKafkaProducer = fizzKafkaProducerFactory.get(apiConfig, config);
                                            if (fizzKafkaProducer != null) {
                                                Flux<SenderResult<Integer>> senderResultFlux = fizzKafkaProducer.sendMessage(sendTopic, message.toJsonString());
                                                return senderResultFlux.flatMap(senderResult -> {
                                                    HttpHeaders headers = getDelegate().getHeaders();
                                                    RecordMetadata metadata = senderResult.recordMetadata();
                                                    if (metadata != null) {
                                                        String metadataStr = String.format("Message %s sent successfully, topic-partition=%s-%s offset=%s timestamp=%s",
                                                                senderResult.correlationMetadata(),
                                                                metadata.topic(),
                                                                metadata.partition(),
                                                                metadata.offset(),
                                                                FizzKafkaProducer.dateFormat.format(new Date(metadata.timestamp()))
                                                        );
                                                        if (logger.isDebugEnabled()) {
                                                            logger.debug("{}\n", metadataStr);
                                                        }
                                                        headers.add(HEADER_FIZZ_PLUGIN_KAFKA_METADATA, metadataStr);
                                                    } else {
                                                        logger.warn("metadata is null =>topic={}, message={}", sendTopic, message);
                                                    }
                                                    headers.add(HEADER_FIZZ_PLUGIN_KAFKA_RESULT, metadata != null ? "1" : "0");
                                                    return Mono.just(remoteResponseBody);
                                                });
                                            } else {
                                                logger.warn("fizzKafkaProducer not found, KafkaMessagePluginFilter.filter failed => {}", exchange);
                                            }
                                            //headers.remove(HttpHeaders.CONTENT_LENGTH);
                                            //NettyDataBuffer from = NettyDataBufferUtils.from("modified body: " + str);
                                            return Mono.just(remoteResponseBody);
                                        }
                                    };
                                    ServerWebExchange decorator = exchange.mutate().response(fizzServerHttpResponseDecorator).build();
                                    return FizzPluginFilterChain.next(decorator); // 执行后续插件或其它逻辑
                                }
                        );
    }
}
