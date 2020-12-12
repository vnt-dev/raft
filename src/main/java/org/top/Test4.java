package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.core.ServerStateTransformerStarter;
import org.top.rpc.utils.PropertiesUtil;

/**
 * @author lubeilin
 * @date 2020/11/5
 */
@Slf4j
public class Test4 {

    public static void main(String[] args) throws InterruptedException {
        PropertiesUtil.setValue("port", "8043");
        PropertiesUtil.setValue("nodes", "127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042,127.0.0.1:8043,127.0.0.1:8044");
        PropertiesUtil.setValue("log", "./data/log4");
        PropertiesUtil.setValue("data", "./data/data4");
        PropertiesUtil.setValue("snapshot", "./data/snapshot4");
        new ServerStateTransformerStarter().start();
    }
}
