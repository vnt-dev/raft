package org.top.core.machine.snapshot;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.top.exception.RaftInitException;
import org.top.rpc.utils.PropertiesUtil;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import static org.top.utils.NumberUtil.*;

/**
 * 快照操作类
 *
 * @author lubeilin
 * @date 2020/12/1
 */
@Slf4j
public class SnapshotLoad {
    /**
     * 最后一个索引
     */
    private final static byte[] LAST_INDEX_KEY = "last_i".getBytes(StandardCharsets.UTF_8);
    /**
     * 任期持久化的key
     */
    private final static byte[] CURRENT_TERM_KEY = "term".getBytes(StandardCharsets.UTF_8);
    private static TransactionDB snapshotDB;

    static {
        RocksDB.loadLibrary();
        init();
    }

    /**
     * 初始化数据
     */
    public static void init() {
        Options options = new Options();
        options.setCreateIfMissing(true);
        TransactionDBOptions dbOptions = new TransactionDBOptions();
        try {
            snapshotDB = TransactionDB.open(options, dbOptions, PropertiesUtil.getString("snapshot"));
        } catch (RocksDBException e) {
            log.error(e.getMessage(), e);
            throw new RaftInitException("快照加载失败");
        }
    }

    /**
     * 重置数据
     *
     * @throws RocksDBException 持久化异常
     */
    public void reset() throws RocksDBException {
        RocksIterator iterator = snapshotDB.newIterator();
        iterator.seekToFirst();
        byte[] first = iterator.key();
        iterator.seekToLast();
        byte[] last = iterator.key();
        snapshotDB.deleteRange(first, last);
        snapshotDB.delete(last);
    }

    /**
     * 设置快照
     *
     * @param key   键
     * @param value 值
     * @param term  任期
     * @param index 索引
     * @throws Exception 执行异常
     */
    public void set(byte[] key, byte[] value, int term, long index) throws Exception {
        Transaction transaction = snapshotDB.beginTransaction(new WriteOptions());
        try {
            transaction.put(key, value);
            transaction.put(CURRENT_TERM_KEY, toBytes(term));
            transaction.put(LAST_INDEX_KEY, toBytes(index));
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    /**
     * 保存任期和索引
     *
     * @param term  任期值
     * @param index 索引
     * @throws Exception
     */
    public void updateTermAndIndex(int term, long index) throws Exception {
        Transaction transaction = snapshotDB.beginTransaction(new WriteOptions());
        try {
            transaction.put(CURRENT_TERM_KEY, toBytes(term));
            transaction.put(LAST_INDEX_KEY, toBytes(index));
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    /**
     * 获取key
     *
     * @param key 键
     * @return 值
     * @throws RocksDBException 获取异常
     */
    public byte[] get(byte[] key) throws RocksDBException {
        return snapshotDB.get(key);
    }

    /**
     * 获取快照存储的任期
     *
     * @return 快照存储的任期
     * @throws Exception 执行异常
     */
    public int getTerm() throws Exception {
        byte[] term = snapshotDB.get(CURRENT_TERM_KEY);
        return term == null ? 0 : toInt(term);
    }

    /**
     * 获取快照的索引值
     *
     * @return 索引值
     * @throws Exception 执行异常
     */
    public long getIndex() throws Exception {
        byte[] index = snapshotDB.get(LAST_INDEX_KEY);
        return index == null ? 0 : toLong(index);
    }

    /**
     * 删除key
     *
     * @param key   key
     * @param term  任期值
     * @param index 索引值
     * @throws Exception 执行异常
     */
    public void del(byte[] key, int term, long index) throws Exception {
        Transaction transaction = snapshotDB.beginTransaction(new WriteOptions());
        try {
            transaction.delete(key);
            transaction.put(CURRENT_TERM_KEY, toBytes(term));
            transaction.put(LAST_INDEX_KEY, toBytes(index));
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    /**
     * 获取一个区域的快照数据
     *
     * @param startKey 起始key
     * @param maxLen   最大长度
     * @return 快照数据集合
     */
    public LinkedList<KvEntity> get(byte[] startKey, int maxLen) {
        RocksIterator rocksIterator = snapshotDB.newIterator();
        if (startKey == null) {
            rocksIterator.seekToFirst();
        } else {
            rocksIterator.seekForPrev(startKey);
            rocksIterator.next();
        }
        LinkedList<KvEntity> linkedList = new LinkedList<>();
        while (linkedList.size() < maxLen && rocksIterator.isValid()) {
            KvEntity kvEntity = new KvEntity(rocksIterator.key(), rocksIterator.value());
            linkedList.addLast(kvEntity);
            rocksIterator.next();
        }
        return linkedList;
    }
}
