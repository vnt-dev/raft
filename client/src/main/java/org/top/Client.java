package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.StringKvOperations;
import org.top.clientapi.util.PropertiesUtil;

/**
 * Hello world!
 *
 */
public class Client
{
    public static void main( String[] args )
    {
        PropertiesUtil.setValue("nodes", "127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042,127.0.0.1:8043");
        PropertiesUtil.setValue("outTime", "5000");
        StringKvOperations operations = new StringKvOperations();
        operations.opsForValue().setIfAbsent("test","11");
        long start = System.currentTimeMillis();
        for (int i=0;i<10;i++){
            operations.opsForValue().incr("test");
        }
        long end = System.currentTimeMillis();
        System.out.println(operations.opsForValue().hasKey("test"));
        System.out.println(end-start);
    }
}
