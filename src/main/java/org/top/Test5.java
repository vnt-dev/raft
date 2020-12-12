package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.core.ServerStateTransformerStarter;
import org.top.rpc.utils.PropertiesUtil;

/**
 * @author lubeilin
 * @date 2020/11/5
 */
@Slf4j
public class Test5 {

    public static void main(String[] args) throws InterruptedException {
        PropertiesUtil.setValue("port", "8044");
        PropertiesUtil.setValue("nodes", "127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042,127.0.0.1:8043,127.0.0.1:8044");
        PropertiesUtil.setValue("log", "./data/log5");
        PropertiesUtil.setValue("data", "./data/data5");
        PropertiesUtil.setValue("snapshot", "./data/snapshot5");
        new ServerStateTransformerStarter().start();
    }
}
