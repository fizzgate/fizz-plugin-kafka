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

import org.apache.kafka.clients.producer.RecordMetadata;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;
import com.fizzgate.plugin.FizzPluginFilterChain;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.plugin.requestbody.RequestBodyPlugin;
import com.fizzgate.spring.http.server.reactive.ext.FizzServerHttpRequestDecorator;
import com.fizzgate.spring.http.server.reactive.ext.FizzServerHttpResponseDecorator;
import com.fizzgate.util.NettyDataBufferUtils;
import com.fizzgate.util.WebUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component(KafkaMessagePluginFilter.KAFKA_MESSAGE_PLUGIN) // 必须，且为插件 id
public class KafkaMessagePluginFilter extends RequestBodyPlugin {
	private static final Logger logger = LoggerFactory.getLogger(KafkaMessagePluginFilter.class);

	public static final String KAFKA_MESSAGE_PLUGIN = "kafkaMessagePlugin"; // 插件 id

	static final String TOPIC_NAME = "topic";
	
	static final String RESP_JSON = "respJson";

	// 阻断原请求
	static final String BLOCK_SOURCE_REQUEST = "blockSourceRequest";

	public static final String HEADER_FIZZ_PLUGIN_KAFKA_METADATA = "fizz-plugin-kafka-metadata";

	public static final String HEADER_FIZZ_PLUGIN_KAFKA_RESULT = "fizz-plugin-kafka-result";

	@Resource(name = "fizzKafkaProducerFactory")
	private com.fizzgate.plugin.message.FizzKafkaProducerFactory fizzKafkaProducerFactory;

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config) {

		FizzServerHttpRequestDecorator request = (FizzServerHttpRequestDecorator) exchange.getRequest();

		return request.getBody().defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER).single().flatMap(body -> {
			String topic = (String) config.get(TOPIC_NAME);
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

			ApiConfig apiConfig = WebUtils.getApiConfig(exchange);
			ServerHttpResponse original = exchange.getResponse();
			// 阻断原请求
			if ("true".equals((String) config.get(BLOCK_SOURCE_REQUEST))) {
				return send(apiConfig, config, sendTopic, message, original).flatMap(isSuccess -> {
					String json = (String) config.get(RESP_JSON);
					return WebUtils.responseJson(exchange, HttpStatus.OK, null, json);
				});
			}

			FizzServerHttpResponseDecorator fizzServerHttpResponseDecorator = new FizzServerHttpResponseDecorator(
					original) {
				@Override
				public Publisher<? extends DataBuffer> writeWith(DataBuffer remoteResponseBody) {
					String responseString = remoteResponseBody.toString(StandardCharsets.UTF_8);
					message.getResponse().setBody(responseString);

					return send(apiConfig, config, sendTopic, message, this).flatMap(isSuccess -> {
						return Mono.just(remoteResponseBody);
					});
				}
			};

			ServerWebExchange decorator = exchange.mutate().response(fizzServerHttpResponseDecorator).build();
			return FizzPluginFilterChain.next(decorator); // 执行后续插件或其它逻辑
		});
	}

	private Mono<Boolean> send(ApiConfig apiConfig, Map<String, Object> config, String sendTopic, Message message,
			ServerHttpResponse resp) {
		com.fizzgate.plugin.message.FizzKafkaProducer fizzKafkaProducer = fizzKafkaProducerFactory.get(apiConfig, config);
		if (fizzKafkaProducer != null) {
			String msgjson = message.toJsonString();
			Flux<SenderResult<Integer>> senderResultFlux = fizzKafkaProducer.sendMessage(sendTopic, msgjson);
			return senderResultFlux.next().flatMap(senderResult -> {
				HttpHeaders headers = resp.getHeaders();
				RecordMetadata metadata = senderResult.recordMetadata();
				if (metadata != null) {
					String metadataStr = String.format(
							"Message %s sent successfully, topic-partition=%s-%s offset=%s timestamp=%s",
							senderResult.correlationMetadata(), metadata.topic(), metadata.partition(),
							metadata.offset(), com.fizzgate.plugin.message.FizzKafkaProducer.dateFormat.format(new Date(metadata.timestamp())));
					if (logger.isDebugEnabled()) {
						logger.debug("{}\n", metadataStr);
					}
					headers.add(HEADER_FIZZ_PLUGIN_KAFKA_METADATA, metadataStr);
					headers.add(HEADER_FIZZ_PLUGIN_KAFKA_RESULT, "1");
					return Mono.just(true);
				} else {
					logger.warn("Failed to send message to kafka, metadata is null =>topic={}, message={}", sendTopic, msgjson);
					headers.add(HEADER_FIZZ_PLUGIN_KAFKA_RESULT, "0");
					return Mono.just(false);
				}
			}).onErrorResume(ex -> {
				logger.warn("kafkaMessagePlugin: Failed to send message to kafka, ex=", ex);
				return Mono.just(false);
			});
		} else {
			logger.warn("fizzKafkaProducer not found, KafkaMessagePluginFilter.filter failed");
		}
		return Mono.just(false);
	}
}
