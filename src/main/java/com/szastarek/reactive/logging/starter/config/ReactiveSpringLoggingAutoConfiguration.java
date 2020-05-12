package com.szastarek.reactive.logging.starter.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.net.ssl.KeyStoreFactoryBean;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import com.szastarek.reactive.logging.starter.filter.ReactiveSpringLoggingFilter;
import com.szastarek.reactive.logging.starter.util.UniqueIDGenerator;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

@Configuration
@ConstructorBinding
@ConfigurationProperties(prefix = "logging.logstash")
public class ReactiveSpringLoggingAutoConfiguration {

	private static final String LOGSTASH_APPENDER_NAME = "LOGSTASH";

	private String url = "localhost:8500";
	private String ignorePatterns;
	private boolean logHeaders;
	private boolean useContentLength = true;
	private String trustStoreLocation;
	private String trustStorePassword;
	@Value("${spring.application.name:-}")
	String name;

	@Bean
	public UniqueIDGenerator generator() {
		return new UniqueIDGenerator();
	}

	@Bean
	public ReactiveSpringLoggingFilter reactiveSpringLoggingFilter() {
		return new ReactiveSpringLoggingFilter(generator(), ignorePatterns, logHeaders, useContentLength);
	}

	@Bean
	@ConditionalOnProperty("logging.logstash.enabled")
	public LogstashTcpSocketAppender logstashAppender() {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		LogstashEncoder encoder = getLogstashEncoder(loggerContext);
		LogstashTcpSocketAppender logstashTcpSocketAppender = new LogstashTcpSocketAppender();
		logstashTcpSocketAppender.setName(LOGSTASH_APPENDER_NAME);
		logstashTcpSocketAppender.setContext(loggerContext);
		logstashTcpSocketAppender.addDestination(url);
		logstashTcpSocketAppender.setEncoder(encoder);
		logstashTcpSocketAppender.start();
		getSSLConfiguration().ifPresent(logstashTcpSocketAppender::setSsl);
		loggerContext.getLogger(ROOT_LOGGER_NAME).addAppender(logstashTcpSocketAppender);
		return logstashTcpSocketAppender;
	}

	private LogstashEncoder getLogstashEncoder(LoggerContext loggerContext) {
		LogstashEncoder encoder = new LogstashEncoder();
		encoder.setContext(loggerContext);
		encoder.setIncludeContext(true);
		encoder.setCustomFields("{\"appname\":\"" + name + "\"}");
		encoder.start();
		return encoder;
	}

	private Optional<SSLConfiguration> getSSLConfiguration() {
		if (trustStoreLocation != null) {
			SSLConfiguration sslConfiguration = new SSLConfiguration();
			KeyStoreFactoryBean factory = new KeyStoreFactoryBean();
			factory.setLocation(trustStoreLocation);
			if (trustStorePassword != null) {
				factory.setPassword(trustStorePassword);
			}
			sslConfiguration.setTrustStore(factory);
			return Optional.of(sslConfiguration);
		}
		return Optional.empty();
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setIgnorePatterns(String ignorePatterns) {
		this.ignorePatterns = ignorePatterns;
	}

	public void setLogHeaders(boolean logHeaders) {
		this.logHeaders = logHeaders;
	}

	public void setUseContentLength(boolean useContentLength) {
		this.useContentLength = useContentLength;
	}

	public void setTrustStoreLocation(String trustStoreLocation) {
		this.trustStoreLocation = trustStoreLocation;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}
}
