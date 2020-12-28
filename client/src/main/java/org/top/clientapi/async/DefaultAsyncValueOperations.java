package org.top.clientapi.async;

import org.top.clientapi.OptionEnum;
import org.top.clientapi.codec.DefaultValueSerializer;
import org.top.clientapi.codec.ValueSerializer;
import org.top.clientapi.util.DataConstants;

import java.util.Arrays;

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
    public void set(String key, V v, Long time) {
        asyncCmdExecutor.cmd(OptionEnum.SET, defaultSer.serialize(key), valueSerializer.serialize(v), time, null);
    }

    @Override
    public void set(String key, V v, ResultCallback<Boolean> resultCallback) {
        this.set(key, v, null, resultCallback);
    }

    @Override
    public void set(String key, V v, Long time, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.SET, defaultSer.serialize(key), valueSerializer.serialize(v), time, new DefaultResponseCallBack<Boolean>(resultCallback) {
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
    public void incr(String key, Long time) {
        asyncCmdExecutor.cmd(OptionEnum.INCR, defaultSer.serialize(key), null, time, null);
    }

    @Override
    public void incr(String key, ResultCallback<Long> resultCallback) {
        this.incr(key, null, resultCallback);
    }

    @Override
    public void incr(String key, Long time, ResultCallback<Long> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.INCR, defaultSer.serialize(key), null, time, new DefaultResponseCallBack<Long>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                String str = defaultSer.deserialize(bytes, String.class);
                resultCallback.success(Long.parseLong(str));
            }
        });
    }

    @Override
    public void incrBy(String key, long v, ResultCallback<Long> resultCallback) {
        this.incrBy(key, v, null, resultCallback);
    }

    @Override
    public void incrBy(String key, long v, Long time, ResultCallback<Long> resultCallback) {
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
    public void decr(String key, Long time) {
        asyncCmdExecutor.cmd(OptionEnum.DECR, defaultSer.serialize(key), null, time, null);
    }

    @Override
    public void decr(String key, ResultCallback<Long> resultCallback) {
        this.decr(key, null, resultCallback);
    }

    @Override
    public void decr(String key, Long time, ResultCallback<Long> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.DECR, defaultSer.serialize(key), null, time, new DefaultResponseCallBack<Long>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                String str = defaultSer.deserialize(bytes, String.class);
                resultCallback.success(Long.parseLong(str));
            }
        });
    }

    @Override
    public void decrBy(String key, long v, ResultCallback<Long> resultCallback) {
        this.decrBy(key, v, null, resultCallback);
    }

    @Override
    public void decrBy(String key, long v, Long time, ResultCallback<Long> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.DECR, defaultSer.serialize(key), defaultSer.serialize(Long.toString(v)), time, new DefaultResponseCallBack<Long>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                String str = defaultSer.deserialize(bytes, String.class);
                resultCallback.success(Long.parseLong(str));
            }
        });
    }

    @Override
    public void setIfAbsent(String key, V v) {
        this.setIfAbsent(key, v, -1L);
    }

    @Override
    public void setIfAbsent(String key, V v, Long time) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_ABSENT, defaultSer.serialize(key), valueSerializer.serialize(v), time, null);
    }

    @Override
    public void setIfAbsent(String key, V v, ResultCallback<Boolean> resultCallback) {
        this.setIfAbsent(key, v, null, resultCallback);
    }

    @Override
    public void setIfAbsent(String key, V v, Long time, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_ABSENT, defaultSer.serialize(key), valueSerializer.serialize(v), new DefaultResponseCallBack<Boolean>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(Arrays.equals(bytes, DataConstants.TRUE));
            }
        });
    }

    @Override
    public void setIfPresent(String key, V v) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_PRESENT, defaultSer.serialize(key), valueSerializer.serialize(v), null, null);
    }

    @Override
    public void setIfPresent(String key, V v, Long time) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_PRESENT, defaultSer.serialize(key), valueSerializer.serialize(v), time, null);
    }

    @Override
    public void setIfPresent(String key, V v, ResultCallback<Boolean> resultCallback) {
        this.setIfPresent(key, v, null, resultCallback);
    }

    @Override
    public void setIfPresent(String key, V v, Long time, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.SET_IF_PRESENT, defaultSer.serialize(key), valueSerializer.serialize(v), null, new DefaultResponseCallBack<Boolean>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(Arrays.equals(bytes, DataConstants.TRUE));
            }
        });
    }

    @Override
    public void hasKey(String key, ResultCallback<Boolean> resultCallback) {
        asyncCmdExecutor.cmd(OptionEnum.HAS_KEY, defaultSer.serialize(key), null, new DefaultResponseCallBack<Boolean>(resultCallback) {
            @Override
            public void success(byte[] bytes) {
                resultCallback.success(Arrays.equals(bytes, DataConstants.TRUE));
            }
        });
    }
}
