package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.StringKvOperations;
import org.top.rpc.utils.PropertiesUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lubeilin
 * @date 2020/11/20
 */
@Slf4j
public class ClientTest {
    public static void main(String[] args) throws InterruptedException {
        PropertiesUtil.setValue("nodes", "127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042,127.0.0.1:8043");
        PropertiesUtil.setValue("outTime", "5000");
        StringKvOperations operations = new StringKvOperations();
        List<Integer> list = new ArrayList<>();
        for (int i=0;i<1000;i++){
            list.add(i);
        }
        log.info("v:{}", operations.opsForValue().get("incr_key 2"));
        long start = System.currentTimeMillis();
        list.parallelStream().forEach(e->{
            operations.opsForValue().set("incr_key 2","");
//            log.info("v:{}", operations.opsForValue().get("incr_key 2"));

//            log.info("v:{}", operations.opsForValue().decrBy("incr_key 2",10));
        });
        long end = System.currentTimeMillis();
        log.info("v:{},time:{}", operations.opsForValue().get("incr_key 2"),end-start);
    }
}
