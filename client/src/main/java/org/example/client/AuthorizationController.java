package org.example.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Controller
//@RestController
public class AuthorizationController {


    public final WebClient defaultClientWebClient;
    private final String messagesBaseUri;


    public AuthorizationController(
            @Qualifier("default-client-web-client") WebClient defaultClientWebClient,
            @Value("${messages.base-uri}") String messagesBaseUri
    ) {
        this.defaultClientWebClient = defaultClientWebClient;
        this.messagesBaseUri = messagesBaseUri;
    }


    @GetMapping(value = "/authorize",params = "grant_type=authorization_code")
    public String authorizationCodeGrant(
            Model model,
            RedirectAttributes redirectAttributes,
            @RegisteredOAuth2AuthorizedClient("messaging-client-authorization-code")OAuth2AuthorizedClient authorizedClient
            ) {
        String messages =  defaultClientWebClient
                .get()
                .uri(this.messagesBaseUri)
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve().bodyToMono(String.class).block();

        redirectAttributes.addFlashAttribute("messages", messages);
         return "redirect:/index";

    }

    @GetMapping(value = "/authorize", params = {"grant_type=client_credentials", "client_auth=mtls"})
    public String clientCredentialsGrantUsingMutualTLS(Model model) {

        String[] messages = this.defaultClientWebClient
                .get()
                .uri(this.messagesBaseUri)
                .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("mtls-demo-client-client-credentials"))
                .retrieve()
                .bodyToMono(String[].class)
                .block();
        model.addAttribute("messages", messages);

        return "index";
    }




}
