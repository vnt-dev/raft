package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.core.ServerStateTransformerStarter;
import org.top.rpc.utils.PropertiesUtil;

/**
 * @author lubeilin
 * @date 2020/11/5
 */
@Slf4j
public class Test2 {

    public static void main(String[] args) {
        PropertiesUtil.setValue("port", "8041");
        PropertiesUtil.setValue("nodes", "127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042");
        PropertiesUtil.setValue("log", "./data/log2");
        PropertiesUtil.setValue("data", "./data/data2");
        PropertiesUtil.setValue("snapshot", "./data/snapshot2");
        new ServerStateTransformerStarter().start();
//        RpcClient.getRpcClient().sendAll(node -> new VoteRequest());
    }
}
