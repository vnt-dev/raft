package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.core.ServerStateTransformerStarter;
import org.top.rpc.utils.PropertiesUtil;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
@Slf4j
public class Server2 {
    public static void main(String[] args) {
        PropertiesUtil.setValue("port", "8042");
        PropertiesUtil.setValue("log", "./data/log2");
        PropertiesUtil.setValue("data", "./data/data2");
        PropertiesUtil.setValue("snapshot", "./data/snapshot2");
        new ServerStateTransformerStarter().start();
    }
}
