package com.szastarek.reactive.logging.starter.util;

import org.springframework.http.HttpStatus;

import java.util.Optional;

public class StatusCodeExtractor {

	public static final String STATUS_CODE_NOT_FOUND_MESSAGE = "STATUS_CODE_NOT_FOUND";

	private StatusCodeExtractor() {
	}

	public static String extractStatusCode(HttpStatus httpStatus) {
		return Optional.ofNullable(httpStatus)
				.map(HttpStatus::toString)
				.orElse(STATUS_CODE_NOT_FOUND_MESSAGE);
	}
}
