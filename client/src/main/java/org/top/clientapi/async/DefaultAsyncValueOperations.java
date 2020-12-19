package org.top.clientapi.async;

import org.top.clientapi.OptionEnum;
import org.top.clientapi.codec.DefaultValueSerializer;
import org.top.clientapi.codec.ValueSerializer;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
public class DefaultAsyncValueOperations<V> implements AsyncValueOperations<V> {
    private ValueSerializer<V> valueSerializer;
    private Class<V> entityClass;
    private AsyncCmdExecutor asyncCmdExecutor = new AsyncCmdExecutor();
    private ValueSerializer<String> defaultSer = new DefaultValueSerializer<>();

    public DefaultAsyncValueOperations(ValueSerializer<V> valueSerializer, Class<V> entityClass) {
        this.valueSerializer = valueSerializer;
        this.entityClass = entityClass;
    }

    @Override
    public void get(String key, ResultCallback<V> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.GET, defaultSer.serialize(key), null, new DefaultResponseCallBack(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                if (bytes == null) {
                    resultCallback.success(null);
                } else {
                    resultCallback.success(valueSerializer.deserialize(bytes, entityClass));
                }
            }
        });
    }

    @Override
    public void delete(String key, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.DEL, defaultSer.serialize(key), null, new DefaultResponseCallBack(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(true);
            }
        });
    }
}
