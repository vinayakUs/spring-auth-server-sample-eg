package org.example.oauth2;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Collections;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Log client certificate
        Certificate[] certs = (Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certs != null && certs.length > 0) {
            X509Certificate cert = (X509Certificate) certs[0];
            log.info("🔐 Client Certificate: Subject={} | Issuer={}", cert.getSubjectDN(), cert.getIssuerDN());
        } else {
            log.warn("⚠️ No client certificate found in request");
        }

        // Proceed with filter chain
        filterChain.doFilter(wrappedRequest, wrappedResponse);

        // Log request details
        log.info("➡️ REQUEST {} {}", request.getMethod(), request.getRequestURI());
        Collections.list(request.getHeaderNames()).forEach(headerName ->
                log.info("🧾 Request Header: {} = {}", headerName, request.getHeader(headerName))
        );
        String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
        if (!requestBody.isBlank()) {
            log.info("📦 Request Body: {}", requestBody);
        }

        // Log response details
        log.info("⬅️ RESPONSE Status: {}", response.getStatus());
        wrappedResponse.getHeaderNames().forEach(headerName ->
                log.info("🧾 Response Header: {} = {}", headerName, wrappedResponse.getHeader(headerName))
        );
        String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
        if (!responseBody.isBlank()) {
            log.info("📦 Response Body: {}", responseBody);
        }

        // Make sure response body is written back to client
        wrappedResponse.copyBodyToResponse();
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
