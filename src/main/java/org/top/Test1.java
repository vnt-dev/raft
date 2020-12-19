package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.core.ServerStateTransformerStarter;
import org.top.rpc.utils.PropertiesUtil;

/**
 * @author lubeilin
 * @date 2020/11/5
 */
@Slf4j
public class Test1 {
    public static void main(String[] args) {
        PropertiesUtil.setValue("port", "8040");
        PropertiesUtil.setValue("nodes", "127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042");
        PropertiesUtil.setValue("log", "./data/log1");
        PropertiesUtil.setValue("data", "./data/data1");
        PropertiesUtil.setValue("snapshot", "./data/snapshot1");
        new ServerStateTransformerStarter().start();
//        NodeGroup.getNodeGroup().forEach(node ->  log.info("{}", node));
//        NodeGroup.getNodeGroup().parallelForEach(node ->  log.info("{}", node));
        //        AsyncFunction<Temp> function = (node,msg)->{
//            log.info("接收：{},msg:{}",node,System.currentTimeMillis()-msg.getStart());
//            node.writeAndFlush(new Temp(msg.getStart(),System.currentTimeMillis()));
//        };
//        new RpcServer().addFunc(function).start();
    }
}
