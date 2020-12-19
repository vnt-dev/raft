package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.BaseKvOperations;
import org.top.rpc.utils.PropertiesUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lubeilin
 * @date 2020/11/20
 */
@Slf4j
public class ClientTest2 {
    public static void main(String[] args) throws InterruptedException {
        PropertiesUtil.setValue("nodes", "127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042,127.0.0.1:8043");
        PropertiesUtil.setValue("outTime", "5000");
        BaseKvOperations<String> operations = new BaseKvOperations<>(String.class);
        List<Integer> list = new ArrayList<>();
        for (int i=0;i<10000;i++){
            list.add(i);
        }
        log.info("v:{}", operations.opsForValue().get("incr_key 2"));
        list.parallelStream().forEach(e->{
            log.info("v:{}", operations.opsForValue().incr("incr_key 2"));
        });
        log.info("v:{}", operations.opsForValue().get("incr_key 2"));
    }
}
