package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.codec.DefaultValueSerializer;
import org.top.clientapi.codec.ValueSerializer;

import java.nio.charset.StandardCharsets;

/**
 * @author lubeilin
 * @date 2020/11/21
 */
@Slf4j
public class Test {

    public static void main(String[] args) throws Exception {
        ValueSerializer<Long> valueSerializer = new DefaultValueSerializer<>();
        String val = "设置环境变量,相当于docker run命dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd令中的-e";
        valueSerializer.serialize(1L);
        "1".getBytes(StandardCharsets.UTF_8);
        long t1 = System.nanoTime();
        byte[] rs1 = valueSerializer.serialize(10L);
        long t2 = System.nanoTime();
        byte[] rs2 = val.getBytes(StandardCharsets.UTF_8);
        long t3 = System.nanoTime();
        System.out.println(t2-t1);
        System.out.println(t3-t2);
    }
}
