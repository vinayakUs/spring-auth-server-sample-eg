package org.example.messageresource;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.X509Certificate;
import java.io.IOException;

@Component
public class ClientCertificateLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ClientCertificateLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request.isSecure() && request.getAttribute("jakarta.servlet.request.X509Certificate") != null) {
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");

            if (certs != null && certs.length > 0) {
                X509Certificate clientCert = certs[0];
                logger.info("🔐 Client certificate subject: {}", clientCert.getSubjectX500Principal());
                logger.info("🔐 Client certificate issuer: {}", clientCert.getIssuerX500Principal());
                logger.info("🔐 Client certificate serial number: {}", clientCert.getSerialNumber());
            } else {
                logger.warn("⚠️ No client certificate found");
            }
        }

        chain.doFilter(request, response);
    }
}
