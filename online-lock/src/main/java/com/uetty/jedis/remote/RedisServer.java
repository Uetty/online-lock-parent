package com.uetty.jedis.remote;

import com.uetty.jedis.lock.DistributedLock;
import com.uetty.jedis.util.LuaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RedisServer {

    final static Logger LOG = LoggerFactory.getLogger(DistributedLock.class);

    private Jedis jedis;
    private int db = 0;

    private ConcurrentMap<String, byte[]> shaCached;

    public RedisServer(Jedis jedis) {
        this.jedis = new Jedis();
        init();
    }

    public RedisServer(final String host) {
        this.jedis = new Jedis(host);
        init();
    }

    public RedisServer(final String host, final int port) {
        this.jedis = new Jedis(host, port);
        init();
    }

    public RedisServer(final String host, final int port, final boolean ssl) {
        this.jedis = new Jedis(host, port, ssl);
        init();
    }

    public RedisServer(final String host, final int port, final boolean ssl,
                       final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
                       final HostnameVerifier hostnameVerifier) {
        this.jedis = new Jedis(host, port, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
        init();
    }

    public RedisServer(URI uri) {
        this.jedis = new Jedis(uri);
        init();
    }

    public RedisServer(URI uri, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
                       final HostnameVerifier hostnameVerifier) {
        this.jedis = new Jedis(uri, sslSocketFactory, sslParameters, hostnameVerifier);
        init();
    }

    private void init() {
        jedis.select(db);
        shaCached = new ConcurrentHashMap<>();
    }

    public void setDb(int db) {
        this.db = db;
        jedis.select(db);
    }

    private byte[] loadLua(String script) {
        byte[] bytes = jedis.scriptLoad(script.getBytes(StandardCharsets.UTF_8));
        LOG.debug("load lua script, sha --> {}", new String(bytes, StandardCharsets.UTF_8));
        return bytes;
    }

    private boolean checkLuaSha(byte[] sha) {
        Long aLong = jedis.scriptExists(sha);
        return aLong != null && aLong > 0;
    }

    public List<Object> execLuas(String luaName, List<List<byte[]>> keys, List<List<byte[]>> values) {
        String script = LuaLoader.getScript(luaName);
        byte[] sha = shaCached.get(script);
        if (sha == null) {
            sha = loadLua(script);
        } else {
            if (!checkLuaSha(sha)) {
                shaCached.remove(script);
                sha = loadLua(script);
                shaCached.putIfAbsent(script, sha);
            }
        }
        Pipeline pipelined = jedis.pipelined();
        for (int i = 0; i < keys.size(); i++) {
            pipelined.evalsha(sha, keys.get(i), values.get(i));
        }
        return pipelined.syncAndReturnAll();
    }

    public List<Object> execByPipeline(Consumer<Pipeline> consumer) {
        Pipeline pipelined = jedis.pipelined();
        consumer.accept(pipelined);
        return pipelined.syncAndReturnAll();
    }

    @Override
    protected void finalize() throws Throwable {
        jedis.disconnect();
        jedis.close();
    }
}
