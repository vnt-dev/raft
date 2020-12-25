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
//        operations.opsForValue().delete("parallelStream");
//        operations.opsForValue().delete("stream");
//        operations.opsForValue().delete("async");
        System.out.println(operations.opsForValue().get("parallelStream"));
        System.out.println(operations.opsForValue().get("stream"));
        System.out.println(operations.opsForValue().get("async"));
        operations.opsForValueAsync().get("async",e->{
            System.out.println(operations.opsForValue().get("async"));
        });
//        operations.opsForValue().setIfPresent("parallelStream", "0");
//        operations.opsForValue().setIfPresent("stream", "0");
//        operations.opsForValue().setIfPresent("async", "0");
//        List<Integer> list = new ArrayList<>();
//        for (int i = 0; i < 100_00; i++) {
//            list.add(i);
//        }
//        for (int i=0;i<100;i++) {
//            long start0 = System.currentTimeMillis();
//            list.parallelStream().forEach(integer -> {
//                operations.opsForValue().incr("parallelStream");
//            });
//            long end0 = System.currentTimeMillis();
//            System.out.println("parallelStream时间：" + (end0 - start0));
//            System.out.println(operations.opsForValue().get("parallelStream"));
//
////        long start = System.currentTimeMillis();
////        list.forEach(integer -> {
////            operations.opsForValue().incr("stream");
////        });
////        long end = System.currentTimeMillis();
////        System.out.println("stream时间：" + (end - start));
////        System.out.println(operations.opsForValue().get("stream"));
//
//            long start1 = System.currentTimeMillis();
//            AtomicLong end1 = new AtomicLong(0);
//            list.forEach(integer -> {
//                operations.opsForValueAsync().incr("async", v -> {
//                    long temp = System.currentTimeMillis();
//                    if (temp > end1.get()) {
//                        end1.set(temp);
//                    }
//                });
//            });
//            long end2 = System.currentTimeMillis();
//
//            Thread.sleep(10000);
//            operations.opsForValueAsync().hasKey("async", System.out::println);
//            System.out.println("async时间：" + (end1.get() - start1));
//            System.out.println("async时间：" + (end2 - start1));
//            System.out.println(operations.opsForValue().get("async"));
//
//
//            System.out.println(operations.opsForValue().get("parallelStream"));
//            System.out.println(operations.opsForValue().get("stream"));
//            System.out.println(operations.opsForValue().get("async"));
//        }
    }
}
