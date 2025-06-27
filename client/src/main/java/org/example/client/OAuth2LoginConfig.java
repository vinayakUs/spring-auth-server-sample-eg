package org.example.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;

import java.net.http.HttpRequest;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class OAuth2LoginConfig {


    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration messaging = ClientRegistration
                .withRegistrationId("messaging-client-oidc")
                .clientId("messaging-client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile")
                .authorizationUri("http://localhost:9000/oauth2/authorize")
                .tokenUri("http://localhost:9000/oauth2/token")
                .userInfoUri("http://localhost:9000/userinfo")
                .jwkSetUri("http://localhost:9000/oauth2/jwks")
                .clientName("messaging-client-oidc")
                .userNameAttributeName("sub")
                .issuerUri("http://localhost:9000")
                .build();


        ClientRegistration messagingClient = ClientRegistration.withRegistrationId("messaging-client-authorization-code")
                .clientId("messaging-client")
                .clientSecret("secret")
                .clientName("messaging-client-authorization-code")
                .authorizationGrantType(new AuthorizationGrantType("authorization_code"))
                .redirectUri("http://127.0.0.1:8080/authorized")
                .scope("message.read", "message.write")
                .authorizationUri("http://localhost:9000/oauth2/authorize") // Replace with your auth server URL
                .tokenUri("http://localhost:9000/oauth2/token")             // Replace with your auth server URL
                .build();

        return new InMemoryClientRegistrationRepository(messaging,messagingClient);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2Login(
                        oauth2Login -> oauth2Login.loginPage("/oauth2/authorization/messaging-client-oidc")
                ).oauth2Client(withDefaults());
        return http.build();
    }

}
