package com.uetty.jedis.config;

import com.uetty.jedis.lock.DistributedLock;
import com.uetty.jedis.remote.RemoteSynchronizer;
import com.uetty.jedis.remote.SimpleRemoteSynchronizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleRemoteConfigure extends RemoteConfigure {

    private static final int DEFAULT_RENEWAL_INTERVAL = 6;
    private static final int DEFAULT_EXPIRE_TIME = 30;
    private static final int DEFAULT_SERVER_PORT = 6379;
    private static final int DEFAULT_BATCH_SIZE = 20;

    private String keyPrefix;

    private String serverHost;
    private int serverPort = DEFAULT_SERVER_PORT;
    private int serverDb;
    private boolean useSSL;
    private URI uri;
    private SSLSocketFactory sslSocketFactory;
    private SSLParameters sslParameters;
    private HostnameVerifier hostnameVerifier;

    private int renewalInterval = DEFAULT_RENEWAL_INTERVAL;
    private int expireTime = DEFAULT_EXPIRE_TIME;

    private int batchSize = DEFAULT_BATCH_SIZE;

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

    public int getRenewalInterval() {
        return renewalInterval;
    }

    public void setRenewalInterval(int renewalInterval) {
        this.renewalInterval = renewalInterval;
    }

    public int getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(int expireTime) {
        this.expireTime = expireTime;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public RemoteSynchronizer createRemoteSynchronizer(ConcurrentHashMap<String, DistributedLock.Sync> lockPool) {
        return new SimpleRemoteSynchronizer(this, lockPool);
    }
}
