package com.szastarek.reactive.logging.starter.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

import static net.logstash.logback.argument.StructuredArguments.value;

public class RequestLoggingInterceptor extends ServerHttpRequestDecorator {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

	private final boolean logHeaders;

	public RequestLoggingInterceptor(ServerHttpRequest delegate, boolean logHeaders) {
		super(delegate);
		this.logHeaders = logHeaders;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return super.getBody().doOnNext(dataBuffer -> {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				Channels.newChannel(baos).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
				String body = new String(baos.toByteArray(), StandardCharsets.UTF_8);
				if (logHeaders) {
					LOGGER.info("Request: method={}, uri={}, headers={}, payload={}, audit={}", getDelegate().getMethod(),
							getDelegate().getPath(), getDelegate().getHeaders(), body, value("audit", true));
				} else {
					LOGGER.info("Request: method={}, uri={}, payload={}, audit={}", getDelegate().getMethod(),
							getDelegate().getPath(), body, value("audit", true));
				}
			}
			catch (IOException e) {
				LOGGER.error(e.getMessage());
			}
		});
	}

}
