package br.com.eduardoreche.insureflow.adapter.in.web;

import io.micrometer.common.KeyValue;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
class InteractionIdFilter extends OncePerRequestFilter {

    static final String HEADER = "x-fapi-interaction-id";

    private static final String MDC_KEY = "interactionId";
    private static final String SPAN_ATTRIBUTE = "interaction.id";
    private static final Pattern UUID_FORMAT =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final HandlerExceptionResolver exceptionResolver;

    InteractionIdFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        boolean malformed = received != null && !UUID_FORMAT.matcher(received).matches();
        String interactionId = received == null || malformed ? UUID.randomUUID().toString() : received;
        response.setHeader(HEADER, interactionId);
        
        ServerHttpObservationFilter.findObservationContext(request)
                .ifPresent(context -> context.addHighCardinalityKeyValue(KeyValue.of(SPAN_ATTRIBUTE, interactionId)));
        
        MDC.put(MDC_KEY, interactionId);
        try {
            if (malformed) {
                // O filtro roda antes do DispatcherServlet e o resolver leva o erro ao ApiExceptionHandler e ao envelope comum.
                exceptionResolver.resolveException(request, response, null, new InvalidInteractionIdException());
                return;
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    static class InvalidInteractionIdException extends RuntimeException {

        InvalidInteractionIdException() {
            super("x-fapi-interaction-id must be a UUID");
        }
    }
}
