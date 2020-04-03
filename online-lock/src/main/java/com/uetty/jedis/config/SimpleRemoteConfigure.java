package com.uetty.jedis.config;

import com.uetty.jedis.remote.RemoteSynchronizer;
import com.uetty.jedis.remote.SimpleRemoteSynchronizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleRemoteConfigure extends RemoteConfigure {

    private String keyPrefix;

    private String serverHost;
    private int serverPort;
    private int serverDb;
    private boolean useSSL;
    private URI uri;
    private SSLSocketFactory sslSocketFactory;
    private SSLParameters sslParameters;
    private HostnameVerifier hostnameVerifier;

    private int maxThreadSize;
    private int initThreadSize;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getServerDb() {
        return serverDb;
    }

    public void setServerDb(int serverDb) {
        this.serverDb = serverDb;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public SSLParameters getSslParameters() {
        return sslParameters;
    }

    public void setSslParameters(SSLParameters sslParameters) {
        this.sslParameters = sslParameters;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public int getMaxThreadSize() {
        return maxThreadSize;
    }

    public void setMaxThreadSize(int maxThreadSize) {
        this.maxThreadSize = maxThreadSize;
    }

    public int getInitThreadSize() {
        return initThreadSize;
    }

    public void setInitThreadSize(int initThreadSize) {
        this.initThreadSize = initThreadSize;
    }

    @Override
    public RemoteSynchronizer createRemoteSynchronizer() {
        return new SimpleRemoteSynchronizer(this);
    }
}
