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

    public void reset() throws RocksDBException {
        RocksIterator iterator = snapshotDB.newIterator();
        iterator.seekToFirst();
        byte[] first = iterator.key();
        iterator.seekToLast();
        byte[] last = iterator.key();
        snapshotDB.deleteRange(first, last);
        snapshotDB.delete(last);
    }

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

    public byte[] get(byte[] key) throws RocksDBException {
        return snapshotDB.get(key);
    }

    public int getTerm() throws Exception {
        byte[] term = snapshotDB.get(CURRENT_TERM_KEY);
        return term == null ? 0 : toInt(term);
    }

    public long getIndex() throws Exception {
        byte[] index = snapshotDB.get(LAST_INDEX_KEY);
        return index == null ? 0 : toLong(index);
    }

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
