package org.top.clientapi;

import lombok.Getter;
import org.top.rpc.entity.SubmitRequest;
import org.top.rpc.entity.SubmitResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * @author lubeilin
 * @date 2020/12/1
 */
public class ResultEntity {
    @Getter
    private String id;
    @Getter
    private Semaphore semaphore;
    @Getter
    private SubmitRequest request;
    @Getter
    private volatile SubmitResponse response;

    private static Map<String, ResultEntity> msgMap = new ConcurrentHashMap<>();

    public static void release(SubmitResponse response) {
        ResultEntity resultEntity = msgMap.get(new String(response.getId(), StandardCharsets.UTF_8));
        if (resultEntity != null) {
            resultEntity.response = response;
            resultEntity.semaphore.release();
        }
    }

    public static void remove(ResultEntity entity) {
        msgMap.remove(entity.id);
    }

    public static ResultEntity getEntity(String option, String key, String value) {
        String id = UUID.randomUUID().toString();
        SubmitRequest submitRequest = new SubmitRequest();
        submitRequest.setOption(option);
        submitRequest.setKey(key.getBytes(StandardCharsets.UTF_8));
        submitRequest.setId(id.getBytes(StandardCharsets.UTF_8));
        if (value != null) {
            submitRequest.setVal(value.getBytes(StandardCharsets.UTF_8));
        }
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.id = id;
        resultEntity.request = submitRequest;
        resultEntity.semaphore = new Semaphore(0);
        msgMap.put(resultEntity.id, resultEntity);
        return resultEntity;
    }
}
