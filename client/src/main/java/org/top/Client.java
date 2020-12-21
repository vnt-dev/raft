package org.top;

import org.top.clientapi.StringKvOperations;
import org.top.clientapi.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hello world!
 */
public class Client {
    public static void main(String[] args) throws InterruptedException {
        PropertiesUtil.setValue("nodes", "127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042,127.0.0.1:8043");
        PropertiesUtil.setValue("outTime", "5000");
        StringKvOperations operations = new StringKvOperations();
        System.out.println(operations.opsForValue().get("parallelStream"));
        System.out.println(operations.opsForValue().get("stream"));
        System.out.println(operations.opsForValue().get("async"));
        operations.opsForValue().setIfPresent("parallelStream", "10");
        operations.opsForValue().setIfPresent("stream", "10");
        operations.opsForValue().setIfPresent("async", "10");
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        long start0 = System.currentTimeMillis();
        list.parallelStream().forEach(integer -> {
            operations.opsForValue().incr("parallelStream");
        });
        long end0 = System.currentTimeMillis();
        System.out.println("parallelStream时间：" + (end0 - start0));
        System.out.println(operations.opsForValue().get("parallelStream"));

        long start = System.currentTimeMillis();
        list.stream().forEach(integer -> {
            operations.opsForValue().incr("stream");
        });
        long end = System.currentTimeMillis();
        System.out.println("stream时间：" + (end - start));
        System.out.println(operations.opsForValue().get("stream"));

        long start1 = System.currentTimeMillis();
        AtomicLong end1 = new AtomicLong(0);
        list.stream().forEach(integer -> {
            operations.opsForValueAsync().incr("async", v -> {
                long temp = System.currentTimeMillis();
                if (temp > end1.get()) {
                    end1.set(temp);
                }
            });
        });
        long end2 = System.currentTimeMillis();

        Thread.sleep(1000);
        System.out.println("async时间：" + (end1.get() - start1));
        System.out.println("async时间：" + (end2 - start1));
        System.out.println(operations.opsForValue().get("async"));
    }
}
