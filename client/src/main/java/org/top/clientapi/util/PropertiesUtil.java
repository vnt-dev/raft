package org.top.clientapi.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lubeilin
 * @date 2020/11/3
 */
@Slf4j
public class PropertiesUtil {
    private static ConcurrentHashMap<String, String> prop = new ConcurrentHashMap<>();

    static {
        init(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
        try {
            init(new FileInputStream(new File("./conf/conf.properties")));
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }

    }

    private static void init(InputStream fis) {
        try {
            if (fis == null) {
                return;
            }
            File confPath = new File("./conf");
            if (!confPath.exists()) {
                confPath.mkdir();
            }
            File conf = new File("./conf/conf.properties");
            if (!conf.exists()) {
                FileOutputStream fos = new FileOutputStream(conf);
                byte[] b = new byte[1024];
                int length;
                while ((length = fis.read(b)) > 0) {
                    fos.write(b, 0, length);
                }
                fos.close();
            }
            Properties properties = new Properties();
            properties.load(fis);
            properties.forEach((k, v) -> prop.put(k.toString(), v.toString()));
            fis.close();
            System.getProperties().forEach((k, v) -> prop.put(k.toString(), v.toString()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public static void setValue(String key, String val) {
        prop.put(key, val);
    }

    public static String getString(String key) {
        return prop.get(key);
    }

    public static String getString(String key, String def) {
        String val = prop.get(key);
        return val == null ? def : val;
    }

    public static int getInt(String key) {
        return Integer.parseInt(prop.get(key));
    }

    public static long getLong(String key) {
        return Long.parseLong(prop.get(key));
    }

    public static int getInt(String key, int def) {
        String val = prop.get(key);
        return val == null ? def : Integer.parseInt(val);
    }
}
