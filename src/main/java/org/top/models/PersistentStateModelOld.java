package org.top.models;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lubeilin
 * @date 2020/11/17
 */
@Getter
@Slf4j
public class PersistentStateModelOld {
//    @Setter
//    private int currentTerm;
//    private LinkedList<LogEntry> logEntries = new LinkedList<>();
//    @Setter
//    private Node votedFor;
//    private final static byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();
//    private final static byte[] CURRENT_TERM_KEY = "CURRENT_TERM_KEY".getBytes();
//    private Serializer<LogEntry> logEntrySerializer = new ProtoBufSerializer<>();
//    private static TransactionDB rocksDB;
//
//    static {
//        RocksDB.loadLibrary();
//        Options options = new Options();
//        options.setCreateIfMissing(true);
//        TransactionDBOptions dbOptions = new TransactionDBOptions();
//        try {
//            rocksDB = TransactionDB.open(options, dbOptions, PropertiesUtil.getString("data"));
//        } catch (RocksDBException e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
//    private static PersistentStateModelOld model = new PersistentStateModelOld();
//
//    private PersistentStateModelOld() {
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        try {
//            byte[] currentTermBytes = getCurrentTerm(transaction);
//            currentTerm = currentTermBytes == null ? 0 : toInt(currentTermBytes);
//            transaction.commit();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static PersistentStateModelOld getModel() {
//        return model;
//    }
//
//    public synchronized int incrementAndGet() throws Exception {
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        byte[] currentTermBytes = getCurrentTerm(transaction);
//        int local = currentTermBytes == null ? 0 : toInt(currentTermBytes);
//        this.currentTerm = local + 1;
//        transaction.put(CURRENT_TERM_KEY, toBytes(currentTerm));
//        transaction.commit();
//        return currentTerm;
//    }
//
//    public synchronized LogEntry getLast() throws Exception {
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        byte[] lastIndexBytes = getLastIndex(transaction);
//        if (lastIndexBytes != null) {
//            int lastIndex = toInt(lastIndexBytes);
//            if (lastIndex != 0) {
//                byte[] bytes = transaction.get(new ReadOptions(), lastIndexBytes);
//                return logEntrySerializer.deserialize(bytes, new LogEntry());
//            }
//        }
//        return new LogEntry();
//    }
//
//    /**
//     * 领导者预提交
//     * 未提交的数据只存在内存中
//     * @param logEntry
//     * @throws Exception
//     */
//    public synchronized void beforehand(LogEntry logEntry) throws Exception {
//        LogEntry last = logEntries.peekLast();
//        if(last==null){
//            last = getLast();
//        }
//        logEntry.setTerm(currentTerm);
//        logEntry.setIndex(last.getIndex() + 1);
//        logEntries.addLast(logEntry);
//    }
//
//
//    public synchronized void pushLast(LogEntry logEntry) throws Exception {
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        byte[] currentTermBytes = getCurrentTerm(transaction);
//        int currentTerm = currentTermBytes == null ? 0 : toInt(currentTermBytes);
//        byte[] lastBytes = getLastIndex(transaction);
//        int lastIndex = lastBytes == null ? 0 : toInt(lastBytes);
//        logEntry.setTerm(currentTerm);
//        logEntry.setIndex(lastIndex + 1);
//        byte[] key = toBytes(lastIndex + 1);
//        transaction.put(LAST_INDEX_KEY, key);
//        transaction.put(key, logEntrySerializer.serialize(logEntry));
//        transaction.commit();
//    }
//
//    public synchronized byte[] getLastIndex(Transaction transaction) throws Exception {
//        return transaction.get(new ReadOptions(), LAST_INDEX_KEY);
//    }
//
//    public byte[] toBytes(int index) throws IOException {
//        ByteArrayOutputStream bao = new ByteArrayOutputStream();
//        DataOutputStream dos = new DataOutputStream(bao);
//        dos.writeInt(index);
//        return bao.toByteArray();
//    }
//
//    public int toInt(byte[] bytes) throws IOException {
//        ByteArrayInputStream bai = new ByteArrayInputStream(bytes);
//        DataInputStream dis = new DataInputStream(bai);
//        return dis.readInt();
//    }
//
//    public synchronized byte[] getCurrentTerm(Transaction transaction) throws Exception {
//        return transaction.get(new ReadOptions(), CURRENT_TERM_KEY);
//    }
//
//    public synchronized void addLogAll(List<LogEntry> append) throws Exception {
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        int appendIndex = 0;
//        for (LogEntry logEntry : append) {
//            byte[] bytes = logEntrySerializer.serialize(logEntry);
//            transaction.put(toBytes(logEntry.getIndex()), bytes);
//            appendIndex = logEntry.getIndex();
//        }
//        transaction.put(LAST_INDEX_KEY, toBytes(appendIndex));
//        transaction.commit();
//    }
//
//    public static void main(String[] args) throws Exception {
//        PersistentStateModelOld model = PersistentStateModelOld.getModel();
//        log.info("index:{}", model.getLogs(0));
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        log.info("index:{}", model.toInt(model.getLastIndex(transaction)));
//    }
//
//    public synchronized LogEntry getLog(int index) throws Exception {
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        byte[] key = toBytes(index);
//        byte[] bytes = transaction.get(new ReadOptions(), key);
//        if (bytes != null) {
//            return logEntrySerializer.deserialize(bytes, new LogEntry());
//        }
//        return null;
//    }
//
//    public synchronized List<LogEntry> getLogs(int index) throws Exception {
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        byte[] lastBytes = getLastIndex(transaction);
//        int lastIndex = lastBytes == null ? 0 : toInt(lastBytes);
//        List<LogEntry> logEntries = new LinkedList<>();
//        for (; index <= lastIndex; index++) {
//            byte[] key = toBytes(index);
//            byte[] bytes = transaction.get(new ReadOptions(), key);
//            if (bytes != null) {
//                logEntries.add(logEntrySerializer.deserialize(bytes, new LogEntry()));
//            }
//        }
//        return logEntries;
//    }
//
//    public synchronized void pollLast() throws Exception {
//        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
//        byte[] lastIndexBytes = transaction.get(new ReadOptions(), LAST_INDEX_KEY);
//        if (lastIndexBytes != null) {
//            int lastIndex = toInt(lastIndexBytes);
//            if (lastIndex != 0) {
//                transaction.delete(lastIndexBytes);
//                transaction.put(LAST_INDEX_KEY, toBytes(lastIndex - 1));
//            }
//        }
//        transaction.commit();
//    }

}
