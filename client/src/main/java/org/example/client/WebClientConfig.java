package org.example.client;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Supplier;

@Configuration(proxyBeanMethods = false)
public class WebClientConfig {

    @Bean("default-client-web-client")
    public WebClient defaultClientWebClient(OAuth2AuthorizedClientManager authorizedClientManager ,SslBundles sslBundles ) throws Exception{
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        return WebClient.builder()
                .clientConnector(createClientConnector(sslBundles.getBundle("demo-client")))
                .apply(oauth2Client.oauth2Configuration())
                .filter(WebClientLoggingFilter.logRequest())
                .filter(WebClientLoggingFilter.logResponse())
                .build();

    }

    public static ClientHttpConnector createClientConnector(SslBundle sslBundle) throws NoSuchAlgorithmException, SSLException {

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


    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository,
            @Qualifier("default-client-http-request-factory") Supplier<ClientHttpRequestFactory> clientHttpRequestFactory,
            RestTemplateBuilder restTemplateBuilder
    ) {

        //create a RestTemplate for token endpoint


        RestTemplate restTemplate = restTemplateBuilder
                .requestFactory(clientHttpRequestFactory)
                .messageConverters(Arrays.asList(
                        new FormHttpMessageConverter(),
                        new OAuth2AccessTokenResponseHttpMessageConverter()
                ))
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .build();

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .clientCredentials(
                        clientCredentialsGrantBuilder -> clientCredentialsGrantBuilder.accessTokenResponseClient(
                                createClientCredentialsTokenResponseClient(restTemplate)
                        )
                )
                .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;

    }


    @Bean
    @Qualifier("default-client-http-request-factory")
    public Supplier<ClientHttpRequestFactory> clientHttpRequestFactory(SslBundles sslBundles) {
        return () -> {
            SslBundle sslBundle = sslBundles.getBundle("demo-client");
            final SSLContext sslContext = sslBundle.createSslContext();
            final SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionSocketFactory)
                    .build();

            final BasicHttpClientConnectionManager connectionManager =
                    new BasicHttpClientConnectionManager(socketFactoryRegistry);
            final CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();
            return new HttpComponentsClientHttpRequestFactory(httpClient);
        };
    }
    private static OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> createClientCredentialsTokenResponseClient(RestTemplate restTemplate){
        DefaultClientCredentialsTokenResponseClient clientCredentialsTokenResponseClient =
                new DefaultClientCredentialsTokenResponseClient();
        clientCredentialsTokenResponseClient.setRestOperations(restTemplate);

        OAuth2ClientCredentialsGrantRequestEntityConverter converter = new OAuth2ClientCredentialsGrantRequestEntityConverter();
        converter.addParametersConverter(
                authorizationGrantRequest->{
                    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
                    // client_id parameter is required for tls_client_auth method
                    parameters.add(OAuth2ParameterNames.CLIENT_ID, authorizationGrantRequest.getClientRegistration().getClientId());
                    return parameters;
                }

        );
        clientCredentialsTokenResponseClient.setRequestEntityConverter(converter);
        return clientCredentialsTokenResponseClient;

    }


}
