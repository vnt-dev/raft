package org.top.clientapi.sync;

import org.top.clientapi.OptionEnum;
import org.top.clientapi.codec.DefaultValueSerializer;
import org.top.clientapi.codec.ValueSerializer;
import org.top.clientapi.exception.RaftException;
import org.top.clientapi.util.DataConstants;

import java.util.Arrays;

/**
 * @author lubeilin
 * @date 2020/12/16
 */
public class DefaultValueOperations<V> implements ValueOperations<V> {

    private ValueSerializer<V> valueSerializer;
    private CmdExecutor cmdExecutor = new CmdExecutor();
    private ValueSerializer<String> defaultSer = new DefaultValueSerializer<>();
    private Class<V> entityClass;

    public DefaultValueOperations(ValueSerializer<V> valueSerializer, Class<V> entityClass) {
        this.valueSerializer = valueSerializer;
        this.entityClass = entityClass;
    }

    @Override
    public void delete(String key) {
        cmdExecutor.cmd(OptionEnum.DEL, defaultSer.serialize(key), null);
    }

    @Override
    public V get(String key) {
        byte[] data = cmdExecutor.cmd(OptionEnum.GET, defaultSer.serialize(key), null);
        if (data == null) {
            return null;
        }
        return valueSerializer.deserialize(data, entityClass);
    }

    @Override
    public void set(String key, V v) {
        this.set(key, v, null);
    }

    @Override
    public void set(String key, V v, Long milliseconds) {
        cmdExecutor.cmd(OptionEnum.SET, defaultSer.serialize(key), valueSerializer.serialize(v), milliseconds);
    }

    @Override
    public long incr(String key) {
        return calculation(OptionEnum.INCR, key, null, null);
    }

    @Override
    public long incr(String key, Long milliseconds) {
        return calculation(OptionEnum.INCR, key, null, milliseconds);
    }

    @Override
    public long incrBy(String key, long val) {
        return calculation(OptionEnum.INCR, key, val, null);
    }

    @Override
    public long incrBy(String key, long val, Long milliseconds) {
        return calculation(OptionEnum.INCR, key, val, milliseconds);
    }

    private long calculation(OptionEnum optionEnum, String key, Long val, Long expire) {
        byte[] value = val == null ? null : defaultSer.serialize(Long.toString(val));
        byte[] data = cmdExecutor.cmd(optionEnum, defaultSer.serialize(key), value, expire);
        try {
            String str = defaultSer.deserialize(data, String.class);
            return Long.parseLong(str);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RaftException("数据错误");
        }
    }

    @Override
    public long decr(String key) {
        return calculation(OptionEnum.DECR, key, null, null);
    }

    @Override
    public long decr(String key, Long milliseconds) {
        return calculation(OptionEnum.DECR, key, null, milliseconds);
    }

    @Override
    public long decrBy(String key, long val) {
        return calculation(OptionEnum.DECR, key, val, null);
    }

    @Override
    public long decrBy(String key, long val, Long milliseconds) {
        return calculation(OptionEnum.DECR, key, val, milliseconds);
    }

    @Override
    public boolean setIfAbsent(String key, V v) {
        return this.setIfAbsent(key, v, null);
    }

    @Override
    public boolean setIfAbsent(String key, V v, Long milliseconds) {
        byte[] bytes = cmdExecutor.cmd(OptionEnum.SET_IF_ABSENT, defaultSer.serialize(key), valueSerializer.serialize(v), milliseconds);
        return Arrays.equals(bytes, DataConstants.TRUE);
    }

    @Override
    public boolean setIfPresent(String key, V v) {
        return this.setIfPresent(key, v, null);
    }

    @Override
    public boolean setIfPresent(String key, V v, Long milliseconds) {
        byte[] bytes = cmdExecutor.cmd(OptionEnum.SET_IF_PRESENT, defaultSer.serialize(key), valueSerializer.serialize(v), milliseconds);
        return Arrays.equals(bytes, DataConstants.TRUE);
    }

    @Override
    public boolean hasKey(String key) {
        byte[] bytes = cmdExecutor.cmd(OptionEnum.HAS_KEY, defaultSer.serialize(key), null);
        return Arrays.equals(bytes, DataConstants.TRUE);
    }
}
