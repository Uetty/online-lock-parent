package com.uetty.jedis.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

public class LuaLoader {

    private static final String luaPath = "/lua/";
    private static ConcurrentHashMap<String, String> SCRIPT_CACHE = new ConcurrentHashMap<>();

    public static String getScript(String luaName) {
        String script = SCRIPT_CACHE.get(luaName);
        if (script == null) {
            InputStream inputStream = LuaLoader.class.getResourceAsStream(luaPath + luaName + ".lua");
            try {
                script = FileUtil.readToString(inputStream);
                SCRIPT_CACHE.putIfAbsent(luaName, script);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return script;
    }
}
