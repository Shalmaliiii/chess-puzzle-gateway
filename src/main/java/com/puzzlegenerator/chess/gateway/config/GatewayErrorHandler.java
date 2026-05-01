package com.puzzlegenerator.chess.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@Order(-2)
public class GatewayErrorHandler extends AbstractErrorWebExceptionHandler {

    public GatewayErrorHandler(ErrorAttributes errorAttributes,
                               WebProperties webProperties,
                               ApplicationContext applicationContext,
                               ServerCodecConfigurer configurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageReaders(configurer.getReaders());
        this.setMessageWriters(configurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorAttributes = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        int statusCode = (int) errorAttributes.getOrDefault("status", 500);
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = switch (status) {
            case NOT_FOUND -> "The requested resource was not found";
            case UNAUTHORIZED -> "Authentication is required";
            case FORBIDDEN -> "Access denied";
            case SERVICE_UNAVAILABLE -> "Downstream service is unavailable";
            default -> "An unexpected error occurred";
        };

        log.error("Gateway error: status={}, path={}, error={}", statusCode, request.path(),
                errorAttributes.get("error"));

        Map<String, Object> responseBody = Map.of(
                "error", status.getReasonPhrase(),
                "status", statusCode,
                "message", message,
                "path", request.path()
        );

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(responseBody));
    }
}
