package com.uetty.jedis.remote;

import com.uetty.jedis.config.RemoteConfigure;
import com.uetty.jedis.config.SimpleRemoteConfigure;

import java.util.Objects;

public class SimpleRemoteSynchronizer extends RemoteSynchronizer {

    private SimpleRemoteConfigure configure;

    public SimpleRemoteSynchronizer(SimpleRemoteConfigure configure) {
        Objects.requireNonNull(configure);
        this.configure = configure;
    }

    @Override
    public void notifySynchronize() {

    }
}
