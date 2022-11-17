package com.example.insecurecommunicationserver;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/*
 * Create cert and copy to app\src\main\assets (or just assets)
 * keytool -genkeypair -alias server -keyalg EC -sigalg SHA384withECDSA -keysize 256 -keystore servercert.p12 -storetype pkcs12 -v -storepass abc123 -validity 10000 -ext san=ip:10.0.2.2,ip:10.0.2.16,ip:127.0.0.1
 */

public class TLSServer {
    public SSLServerSocket serve(int port, String tlsVersion, String trustStoreName,
                      char[] trustStorePassword, String keyStoreName, char[] keyStorePassword)
            throws Exception {

        Objects.requireNonNull(tlsVersion, "TLS version is mandatory");

        if (port <= 0) {
            throw new IllegalArgumentException(
                    "Port number cannot be less than or equal to 0");
        }

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        InputStream ts = MainActivity.getAppContext().getAssets().open(trustStoreName);

        trustStore.load(ts, trustStorePassword);
        ts.close();
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream ks = MainActivity.getAppContext().getAssets().open(keyStoreName);

        keyStore.load(ks, keyStorePassword);
        //ks.close();
        KeyManagerFactory kmf = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword);
        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
                SecureRandom.getInstanceStrong());

        SSLServerSocketFactory factory = ctx.getServerSocketFactory();
        ServerSocket listener;
        SSLServerSocket sslListener = null;
        try {
            listener = factory.createServerSocket(port);
            sslListener = (SSLServerSocket) listener;

            sslListener.setNeedClientAuth(true);
            sslListener.setEnabledProtocols(new String[] {tlsVersion});
        } catch (Exception e ) {
            e.printStackTrace();
        }
        return sslListener;
    }
}