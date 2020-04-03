package com.uetty.jedis.test;

import redis.clients.jedis.Jedis;

public class JedisTest {

    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.select(0);

        jedis.set("test1", "223444");

    }
}
