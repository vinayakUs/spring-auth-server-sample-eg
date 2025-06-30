package org.example.client;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import com.nimbusds.jose.util.Base64;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.Ssl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
public class JwkSetController {

    private final JWKSet jwkSet;
    public JwkSetController(SslBundles sslBundles) throws Exception {
        this.jwkSet = initJwkSet(sslBundles);
    }
    @GetMapping({"/jwks"})
    public Map<String, Object> getJwkSet() {
        return this.jwkSet.toJSONObject();
    }
    private JWKSet initJwkSet(SslBundles sslBundles) throws Exception {
        SslBundle sslBundle = sslBundles.getBundle("demo-client");
        KeyStore keyStore = sslBundle.getStores().getKeyStore();
        String alias = sslBundle.getKey().getAlias();
        Certificate certificate = keyStore.getCertificate(alias);
        RSAKey rsaKey = (new RSAKey.Builder((RSAPublicKey)certificate.getPublicKey())).keyUse(KeyUse.SIGNATURE).keyID(UUID.randomUUID().toString()).x509CertChain(Collections.singletonList(Base64.encode(certificate.getEncoded()))).build();
        return new JWKSet(rsaKey);
    }

}
