package org.example.client;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import java.security.NoSuchAlgorithmException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

@Configuration(proxyBeanMethods = false)
public class WebClientConfig {

    @Bean("default-client-web-client")
    public WebClient defaultClientWebClient(OAuth2AuthorizedClientManager authorizedClientManager ,SslBundles sslBundles ) throws Exception{
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        return WebClient.builder()
                .clientConnector(createClientConnector(sslBundles.getBundle("demo-client")))
                .apply(oauth2Client.oauth2Configuration())
                .build();

    }

    public ClientHttpConnector createClientConnector(SslBundle sslBundle ) throws NoSuchAlgorithmException, SSLException {

        KeyManagerFactory keyManagerFactory = sslBundle.getManagers().getKeyManagerFactory();
        TrustManagerFactory trustManagerFactory = sslBundle.getManagers().getTrustManagerFactory();

        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(keyManagerFactory)
                .trustManager(trustManagerFactory)
                .build();

        SslProvider sslProvider = SslProvider.builder().sslContext(sslContext).build();
        HttpClient httpClient = HttpClient.create().secure(sslProvider);
        return new ReactorClientHttpConnector(httpClient);
    }
}
