package org.top.clientapi.async;

import lombok.Getter;
import org.top.clientapi.entity.SubmitRequest;
import org.top.clientapi.entity.SubmitResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lubeilin
 * @date 2020/12/1
 */
public class AsyncResultEntity {
    private static Map<String, AsyncResultEntity> msgMap = new ConcurrentHashMap<>();
    @Getter
    private String id;
    @Getter
    private SubmitRequest request;
    private volatile ResponseCallback responseCallback;

    public static boolean async(SubmitResponse response) {
        AsyncResultEntity asyncResultEntity = msgMap.remove(response.getId());
        if (asyncResultEntity != null) {
            if (asyncResultEntity.responseCallback != null) {
                asyncResultEntity.responseCallback.callback(response.getCode(), response.getData());
            }
            return true;
        }
        return false;
    }

    public static AsyncResultEntity getEntity(String option, byte[] key, byte[] value, ResponseCallback responseCallback) {
        if (key == null) {
            throw new RuntimeException("key不能为空");
        }
        String id = UUID.randomUUID().toString();
        SubmitRequest submitRequest = new SubmitRequest();
        submitRequest.setOption(option);
        submitRequest.setKey(key);
        submitRequest.setId(id);
        submitRequest.setVal(value);
        AsyncResultEntity asyncResultEntity = new AsyncResultEntity();
        asyncResultEntity.id = id;
        asyncResultEntity.request = submitRequest;
        asyncResultEntity.responseCallback = responseCallback;
        msgMap.put(asyncResultEntity.id, asyncResultEntity);
        return asyncResultEntity;
    }
}
