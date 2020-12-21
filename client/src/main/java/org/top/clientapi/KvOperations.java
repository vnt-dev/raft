package org.top.clientapi;

import org.top.clientapi.async.AsyncValueOperations;
import org.top.clientapi.sync.ValueOperations;

/**
 * @author lubeilin
 * @date 2020/12/15
 */
public interface KvOperations<V> {

    /**
     * kv 操作
     *
     * @return
     */
    ValueOperations<V> opsForValue();

    /**
     * 异步操作
     *
     * @return
     */
    AsyncValueOperations<V> opsForValueAsync();
}
