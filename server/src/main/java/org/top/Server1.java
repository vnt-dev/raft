package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.core.ServerStateTransformerStarter;
import org.top.rpc.utils.PropertiesUtil;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
@Slf4j
public class Server1 {
    public static void main(String[] args) {
        PropertiesUtil.setValue("port","8041");
        PropertiesUtil.setValue("log","./data/log1");
        PropertiesUtil.setValue("data","./data/data1");
        PropertiesUtil.setValue("snapshot","./data/snapshot1");
        new ServerStateTransformerStarter().start();
    }
}
