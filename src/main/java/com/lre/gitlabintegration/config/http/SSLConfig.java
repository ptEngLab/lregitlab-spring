package com.lre.gitlabintegration.config.http;

import com.lre.gitlabintegration.exceptions.LreException;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;

@Configuration
public class SSLConfig {

    @Value("${http.client.ca.path:cacert.crt}")
    private String caCertPath;

    @Bean
    public SSLContext sslContext() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certificates = loadCertificates(cf);
            KeyStore keystore = createKeyStore(certificates);
            return createSSLContext(keystore);
        } catch (CertificateException e) {
            throw new LreException(
                    "Failed to load X.509 certificates from " + caCertPath,
                    e
            );
        } catch (KeyStoreException e) {
            throw new LreException(
                    "Failed to create or configure KeyStore",
                    e
            );
        } catch (NoSuchAlgorithmException e) {
            throw new LreException(
                    "Required cryptographic algorithm not available",
                    e
            );
        } catch (IOException e) {
            throw new LreException(
                    "I/O error while loading certificate file: " + caCertPath,
                    e
            );
        }
    }

    private Collection<? extends Certificate> loadCertificates(
            CertificateFactory cf
    ) throws IOException, CertificateException {

        try (InputStream is =
                     getClass().getClassLoader().getResourceAsStream(caCertPath)) {

            if (is == null) {
                throw new LreException(caCertPath + " not found in classpath");
            }

            return cf.generateCertificates(is);
        }
    }

    private KeyStore createKeyStore(
            Collection<? extends Certificate> certificates
    ) throws KeyStoreException, IOException, NoSuchAlgorithmException,
             CertificateException {

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null, null);

        int index = 1;
        for (Certificate cert : certificates) {
            keystore.setCertificateEntry("ca-" + index++, cert);
        }

        return keystore;
    }

    private SSLContext createSSLContext(KeyStore keystore) {
        try {
            return SSLContexts.custom()
                    .loadTrustMaterial(keystore, null)
                    .build();
        } catch (Exception e) {
            throw new LreException("Failed to build SSL context", e);
        }
    }
}
