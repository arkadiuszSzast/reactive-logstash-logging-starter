package com.szastarek.reactive.logging.starter.interceptor;

import com.szastarek.reactive.logging.starter.util.StatusCodeExtractor;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

import static net.logstash.logback.argument.StructuredArguments.value;

public class ResponseLoggingInterceptor extends ServerHttpResponseDecorator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseLoggingInterceptor.class);

	private final long startTime;
	private final boolean logHeaders;

	public ResponseLoggingInterceptor(ServerHttpResponse delegate, long startTime, boolean logHeaders) {
		super(delegate);
		this.startTime = startTime;
		this.logHeaders = logHeaders;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		Flux<DataBuffer> buffer = Flux.from(body);
		return super.writeWith(buffer.doOnNext(dataBuffer -> {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				Channels.newChannel(baos).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
				String bodyRes = new String(baos.toByteArray(), StandardCharsets.UTF_8);
				if (logHeaders) {
					LOGGER.info("Response({} ms): status={}, headers={}, payload={}, audit={}", value("X-Response-Time", System.currentTimeMillis() - startTime),
							value("X-Response-Status", StatusCodeExtractor.extractStatusCode(getStatusCode())), getDelegate().getHeaders(), bodyRes, value("audit", true));
				} else {
					LOGGER.info("Response({} ms): status={}, payload={}, audit={}", value("X-Response-Time", System.currentTimeMillis() - startTime),
							value("X-Response-Status", StatusCodeExtractor.extractStatusCode(getStatusCode())), bodyRes, value("audit", true));
				}
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
			}
		}));
	}
}
