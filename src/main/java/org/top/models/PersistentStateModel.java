package org.top.models;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.top.core.RaftServerData;
import org.top.core.machine.snapshot.SnapshotLoad;
import org.top.exception.RaftException;
import org.top.rpc.Node;
import org.top.rpc.codec.ProtoBufSerializer;
import org.top.rpc.codec.Serializer;
import org.top.rpc.utils.PropertiesUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.top.utils.NumberUtil.*;

/**
 * @author lubeilin
 * @date 2020/11/17
 */

@Slf4j
public class PersistentStateModel {
    /**
     * 最后一个索引
     */
    private final static byte[] LAST_INDEX_KEY = "last_index".getBytes(StandardCharsets.UTF_8);
    /**
     * 任期持久化的key
     */
    private final static byte[] CURRENT_TERM_KEY = "current_term".getBytes(StandardCharsets.UTF_8);
    /**
     * 当前任期给某节点投票的key
     */
    private final static byte[] VOTED_FOR_KEY = "voted_for".getBytes(StandardCharsets.UTF_8);
    private final static byte[] LOG = "log".getBytes(StandardCharsets.UTF_8);
    private static TransactionDB rocksDB;

    static {
        RocksDB.loadLibrary();
        init();
    }

    private static PersistentStateModel model = new PersistentStateModel();

    private Serializer<LogEntry> logEntrySerializer = new ProtoBufSerializer<>();

    private PersistentStateModel() {
    }

    private static void init() {
        Options options = new Options();
        options.setCreateIfMissing(true);
        TransactionDBOptions dbOptions = new TransactionDBOptions();
        try {
            rocksDB = TransactionDB.open(options, dbOptions, PropertiesUtil.getString("log"));
        } catch (RocksDBException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static PersistentStateModel getModel() {
        return model;
    }


    public LogEntry getLast() throws Exception {
        byte[] lastIndexBytes = rocksDB.get(LAST_INDEX_KEY);
        if (lastIndexBytes != null) {
            return getLog(lastIndexBytes);
        }
        return new LogEntry();
    }


    public void pushLast(LogEntry logEntry) throws Exception {
        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
        RaftServerData.lock.lock();
        try {
            int currentTerm = getCurrentTerm();
            long lastIndex = getLastIndex();
            logEntry.setIndex(lastIndex + 1);
            logEntry.setTerm(currentTerm);
            byte[] key = toBytes(logEntry.getIndex());
            transaction.put(LAST_INDEX_KEY, key);
            pushLog(key, logEntry, transaction);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            RaftServerData.lock.unlock();
        }
    }

    public long getLastIndex() throws Exception {
        byte[] bytes = rocksDB.get(LAST_INDEX_KEY);
        return bytes == null ? 0L : toLong(bytes);
    }


    public void setCurrentTerm(int currentTerm, Transaction transaction) throws Exception {
        transaction.put(CURRENT_TERM_KEY, toBytes(currentTerm));
    }

//    public static void main(String[] args) throws RocksDBException {
//        rocksDB.put(handle,"1".getBytes(),"2".getBytes());
//        rocksDB.put(handle,"2".getBytes(),"2".getBytes());
//        rocksDB.put(handle,"3".getBytes(),"2".getBytes());
//        log.info("{}",rocksDB.get("1".getBytes()));
//        log.info("{}",rocksDB.get(handle,"1".getBytes()));
//        log.info("{}",rocksDB.get(handle,"2".getBytes()));
//        log.info("{}",rocksDB.get(handle,"3".getBytes()));
//        rocksDB.delete();
//        log.info("{}",rocksDB.get(handle,"3".getBytes()));
//    }

    public int getCurrentTerm() throws Exception {
        byte[] bytes = rocksDB.get(new ReadOptions(), CURRENT_TERM_KEY);
        return bytes == null ? 0 : toInt(bytes);
    }

    /**
     * 增加任期
     *
     * @param transaction 持久化操作类
     * @return 增加后的值
     * @throws Exception 操作异常
     */
    public int incrementAndGet(Transaction transaction) throws Exception {
        int currentTerm = getCurrentTerm();
        transaction.put(CURRENT_TERM_KEY, toBytes(currentTerm + 1));
        return currentTerm;
    }

    /**
     * 设置投票者
     *
     * @param node        候选者id
     * @param transaction 操作类
     * @throws Exception 操作异常
     */
    public void setVotedFor(Node node, Transaction transaction) throws Exception {
        if (node == null) {
            transaction.delete(VOTED_FOR_KEY);
        } else {
            transaction.put(VOTED_FOR_KEY, node.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 获取投票者
     *
     * @return 已投票的人
     * @throws Exception 操作异常
     */
    public Node getVotedFor() throws Exception {
        byte[] bytes = rocksDB.get(new ReadOptions(), VOTED_FOR_KEY);
        if (bytes == null) {
            return null;
        } else {
            return new Node(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    /**
     * 接收快照之后追加日志
     *
     * @param append 日志条目，按索引从小到大排序
     * @throws Exception 操作异常
     */
    public void addLogAll(List<LogEntry> append) throws Exception {
        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
        try {
            long lastIndex = 0;
            for (LogEntry logEntry : append) {
                pushLog(toBytes(logEntry.getIndex()), logEntry, transaction);
                lastIndex = logEntry.getIndex();
            }
            transaction.put(LAST_INDEX_KEY, toBytes(lastIndex));
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    /**
     * 丢弃日志,把最后索引标记为0即可
     *
     * @throws RocksDBException 操作异常
     */
    public void reset() throws RocksDBException, IOException {
        rocksDB.put(LAST_INDEX_KEY, toBytes(0L));
    }

    private LogEntry getLog(byte[] index) throws Exception {
        byte[] key = Arrays.copyOf(LOG, index.length + LOG.length);
        System.arraycopy(index, 0, key, LOG.length, index.length);
        byte[] bytes = rocksDB.get(key);
        if (bytes != null) {
            return logEntrySerializer.deserialize(bytes, new LogEntry());
        }
        if (toLong(index) == 0) {
            return new LogEntry();
        }
        log.info("index:{},max:{}，raft:{}，min:{}", toLong(index), getLastIndex(), RaftServerData.serverState, new SnapshotLoad().getIndex());
        throw new RaftException("数据异常+");
    }

    private void pushLog(byte[] index, LogEntry logEntry, Transaction transaction) throws Exception {
        byte[] key = Arrays.copyOf(LOG, index.length + LOG.length);
        System.arraycopy(index, 0, key, LOG.length, index.length);
        byte[] bytes = logEntrySerializer.serialize(logEntry);
        transaction.put(key, bytes);
    }

    private void deleteLog(byte[] index, Transaction transaction) throws Exception {
        byte[] key = Arrays.copyOf(LOG, index.length + LOG.length);
        System.arraycopy(index, 0, key, LOG.length, index.length);
        transaction.delete(key);
    }

    public LogEntry getLog(long index) throws Exception {
        byte[] indexKey = toBytes(index);
        return getLog(indexKey);
    }

    /**
     * 获取日志区间
     *
     * @param index    索引起点 包含
     * @param maxIndex 索引终点 不包含
     * @return 区间内的日志列表
     * @throws Exception 存储异常
     */
    public LinkedList<LogEntry> getLogs(long index, long maxIndex) throws Exception {
        long lastIndex = getLastIndex();
        LinkedList<LogEntry> logEntries = new LinkedList<>();
        for (; index <= lastIndex && index < maxIndex; index++) {
            LogEntry logEntry = getLog(index);
            logEntries.add(logEntry);
        }
        return logEntries;
    }


    public Transaction begin() {
        return rocksDB.beginTransaction(new WriteOptions());
    }

    /**
     * 附加日志并解决冲突
     *
     * @param append      日志是有序的，按索引从小到大排列
     * @param commitIndex 已提交的不需要再判断
     * @throws Exception 操作异常
     */
    public void addLogs(List<LogEntry> append, long commitIndex) throws Exception {
        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
        try {
            long lastIndex = getLastIndex();
            for (LogEntry logEntry : append) {
                if (logEntry.getIndex() > lastIndex) {
                    //追加日志中尚未存在的任何新条目
                    lastIndex = logEntry.getIndex();
                    pushLog(toBytes(lastIndex), logEntry, transaction);
                } else if (logEntry.getIndex() > commitIndex && getLog(logEntry.getIndex()).getTerm() != logEntry.getTerm()) {
                    //发生了冲突（因为索引相同，任期不同），那么就删除这个已经存在的条目以及它之后的所有条目,这里只做标记即可，后面直接覆盖
                    lastIndex = logEntry.getIndex();
                    pushLog(toBytes(lastIndex), logEntry, transaction);
                }
            }
            transaction.put(LAST_INDEX_KEY, toBytes(lastIndex));
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }

    }

    public void remove(long index) throws Exception {
        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
        try {
            deleteLog(toBytes(index), transaction);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

}
