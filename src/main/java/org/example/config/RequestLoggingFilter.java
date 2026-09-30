package org.example.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final int MAX_BODY_LENGTH = 1000;

    private String getStringValue(byte[] content, String characterEncoding) {
        Charset charset = characterEncoding != null ? Charset.forName(characterEncoding) : StandardCharsets.UTF_8;
        String body = new String(content, charset);
        return body.length() > MAX_BODY_LENGTH ? body.substring(0, MAX_BODY_LENGTH) + "...(truncated)" : body;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, MAX_BODY_LENGTH + 1);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long timeTaken = System.currentTimeMillis() - startTime;
            int status = responseWrapper.getStatus();

            // Never log the Authorization header: it carries the Basic auth credentials
            String summary = "API CLIENT REQUEST: CLIENT={}; METHOD={}; ENDPOINT={}; PARAMS={}; RESPONSE CODE={}; DURATION={}ms";
            Object[] args = {request.getRemoteHost(), request.getMethod(), request.getRequestURI(),
                    request.getQueryString(), status, timeTaken};
            if (status >= 500) {
                LOGGER.error(summary, args);
            } else if (status >= 400) {
                LOGGER.warn(summary, args);
            } else {
                LOGGER.info(summary, args);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("REQUEST BODY={}; RESPONSE={}",
                        getStringValue(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding()),
                        getStringValue(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding()));
            }

            responseWrapper.copyBodyToResponse();
        }
    }
}
