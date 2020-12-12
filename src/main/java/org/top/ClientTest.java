package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.KvUtils;
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
        KvUtils kvUtils = new KvUtils();
        log.info("get");
        log.info("rs:{}", kvUtils.get("hello world"));

        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

//        log.info("{}: {}",999, kvUtils.get("k"+999));
        long start = System.currentTimeMillis();
        log.info("start");
        list.stream().forEach(v -> {
            kvUtils.set(""+v,""+v); //2835
//            kvUtils.delete(""+v);//1951
//            if(!("" + v).equals( kvUtils.get("" + v)))
//            log.info("key:{},v:{}", v, kvUtils.get("" + v)); //639
        });
        log.info("end,:{}", System.currentTimeMillis() - start);
        log.info("rs:{}", kvUtils.get("hello world"));
    }
}
