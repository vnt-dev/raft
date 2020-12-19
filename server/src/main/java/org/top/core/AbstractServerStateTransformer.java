package org.top.core;

import org.top.exception.RaftException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lubeilin
 * @date 2020/11/10
 */
public abstract class AbstractServerStateTransformer implements ServerStateTransformer {

    private static Map<ServerStateEnum, ServerStateTransformer> map = new HashMap<>();

    public static ServerStateTransformer getServer(ServerStateEnum stateEnum) {
        return map.computeIfAbsent(stateEnum, serverStateEnum -> {
            switch (stateEnum) {
                case LEADER:
                    return new LeaderStateImpl();
                case FOLLOWER:
                    return new FollowerStateImpl();
                case CANDIDATE:
                    return new CandidateStateImpl();
                default:
                    throw new RaftException("状态错误");
            }
        });
    }

    public void executeNext() throws Exception {
        ServerStateEnum nextState = nextState();
        executeNext(nextState);
    }

    public void executeNext(ServerStateEnum nextState) throws Exception {
        ServerStateTransformer transformer = getServer(nextState);
        if (transformer.pro()) {
            transformer.execute();
        } else {
            executeNext(transformer.nextState());
        }
    }
}
