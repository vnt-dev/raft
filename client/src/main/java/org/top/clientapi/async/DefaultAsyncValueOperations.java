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
        asyncCmdExecutor.cmd(OptionEnum.GET, defaultSer.serialize(key), null, new DefaultResponseCallBack<V>(resultCallback) {
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
        asyncCmdExecutor.cmd(OptionEnum.DEL, defaultSer.serialize(key), null, new DefaultResponseCallBack<Boolean>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(true);
            }
        });
    }

    @Override
    public void delete(String key) {
        asyncCmdExecutor.cmd(OptionEnum.DEL, defaultSer.serialize(key), null, null);
    }

    @Override
    public void set(String key, V v) {
        asyncCmdExecutor.cmd(OptionEnum.SET, defaultSer.serialize(key), valueSerializer.serialize(v), null);
    }

    @Override
    public void set(String key, V v, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.SET, defaultSer.serialize(key), valueSerializer.serialize(v), new DefaultResponseCallBack<Boolean>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(true);
            }
        });
    }

    @Override
    public void incr(String key) {
        asyncCmdExecutor.cmd(OptionEnum.INCR, defaultSer.serialize(key), null, null);
    }

    @Override
    public void incr(String key, ResultCallback<Long> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.INCR, defaultSer.serialize(key), null, new DefaultResponseCallBack<Long>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                String str = defaultSer.deserialize(bytes, String.class);
                resultCallback.success(Long.parseLong(str));
            }
        });
    }

    @Override
    public void incrBy(String key, long v, ResultCallback<Long> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.INCR, defaultSer.serialize(key), defaultSer.serialize(Long.toString(v)), new DefaultResponseCallBack<Long>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                String str = defaultSer.deserialize(bytes, String.class);
                resultCallback.success(Long.parseLong(str));
            }
        });
    }

    @Override
    public void decr(String key) {
        asyncCmdExecutor.cmd(OptionEnum.DECR, defaultSer.serialize(key), null, null);
    }

    @Override
    public void decr(String key, ResultCallback<Long> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.DECR, defaultSer.serialize(key), null, new DefaultResponseCallBack<Long>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                String str = defaultSer.deserialize(bytes, String.class);
                resultCallback.success(Long.parseLong(str));
            }
        });
    }

    @Override
    public void decrBy(String key, long v, ResultCallback<Long> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.DECR, defaultSer.serialize(key), defaultSer.serialize(Long.toString(v)), new DefaultResponseCallBack<Long>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                String str = defaultSer.deserialize(bytes, String.class);
                resultCallback.success(Long.parseLong(str));
            }
        });
    }

    @Override
    public void setIfAbsent(String key, V v) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_ABSENT, defaultSer.serialize(key), valueSerializer.serialize(v), null);
    }

    @Override
    public void setIfAbsent(String key, V v, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_ABSENT, defaultSer.serialize(key), valueSerializer.serialize(v), new DefaultResponseCallBack<Boolean>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(true);
            }
        });
    }

    @Override
    public void setIfPresent(String key, V v) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_PRESENT, defaultSer.serialize(key), valueSerializer.serialize(v), null);
    }

    @Override
    public void setIfPresent(String key, V v, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_PRESENT, defaultSer.serialize(key), valueSerializer.serialize(v), new DefaultResponseCallBack<Boolean>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(true);
            }
        });
    }

    @Override
    public void hasKey(String key, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.HAS_KEY, defaultSer.serialize(key), null, new DefaultResponseCallBack<Boolean>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(true);
            }
        });
    }
}
