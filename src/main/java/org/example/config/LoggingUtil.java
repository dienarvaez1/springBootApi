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
import java.io.UnsupportedEncodingException;

@Component
public class LoggingUtil extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUtil.class);


    private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
        try {
            return new String(contentAsByteArray, 0, contentAsByteArray.length, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        filterChain.doFilter(requestWrapper, responseWrapper);
        long timeTaken = System.currentTimeMillis() - startTime;

        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(),
                request.getCharacterEncoding());
        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(),
                response.getCharacterEncoding());

        int resCode = (response.getStatus());

        //String resTrailer = String.valueOf((response.getHeader("Set-Cookie")));

        if (resCode == 200) {
            LOGGER.info
                    ( "API CLIENT REQUEST: CLIENT={}; AUTH={}; METHOD={}; TRAILERS={}; ENDPOINT={}; PARAMS={}; RESPONSE CODE={}; DURATION={}; REQUEST BODY={}; RESPONSE={}",
                            request.getRemoteHost(), request.getHeader("Authorization"), request.getMethod(), response.getTrailerFields(), request.getRequestURI(), request.getParameterMap(), response.getStatus(), timeTaken, requestBody, responseBody
                    );
        } else {
            LOGGER.error
                    ( "API CLIENT REQUEST: CLIENT={}; AUTH={}; METHOD={}; TRAILERS={}; ENDPOINT={}; PARAMS={}; RESPONSE CODE={}; DURATION={}; REQUEST BODY={}; RESPONSE={}",
                            request.getRemoteHost(), request.getHeader("Authorization"), request.getMethod(), response.getTrailerFields(), request.getRequestURI(), request.getParameterMap(), response.getStatus(), timeTaken, requestBody, responseBody
                    );
        }

        responseWrapper.copyBodyToResponse();
    }
}