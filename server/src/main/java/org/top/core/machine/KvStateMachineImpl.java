package org.top.core.machine;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.top.clientapi.entity.OperationState;
import org.top.core.RaftServerData;
import org.top.core.machine.snapshot.KvEntity;
import org.top.core.machine.snapshot.SnapshotLoad;
import org.top.exception.RaftException;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.rpc.codec.ProtoBufSerializer;
import org.top.rpc.codec.Serializer;
import org.top.rpc.utils.PropertiesUtil;
import org.top.utils.DataConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 状态机中的数据一定是已提交的，但是从节点的数据会有延迟
 *
 * @author lubeilin
 * @date 2020/11/12
 */
@Slf4j
public class KvStateMachineImpl implements StateMachine, SnapshotService {
    /**
     * key-value结构
     */
    private final static byte[] STRING = "string".getBytes(StandardCharsets.UTF_8);
    /**
     * 指令执行序列号
     */
    private final static byte[] SERIAL_NUMBER_PRE = "serial_num".getBytes(StandardCharsets.UTF_8);
    private final static byte[] SERIAL_NUMBER_VAL = "val".getBytes(StandardCharsets.UTF_8);
    private static ProtoBufSerializer<String> serializer = new ProtoBufSerializer<>();
    private static ProtoBufSerializer<DefaultValue> cmdValueSerializer = new ProtoBufSerializer<>();
    private Serializer<CompareValue> compareValueSerializer = new ProtoBufSerializer<>();
    private static TransactionDB rocksDB;

    static {
        RocksDB.loadLibrary();
        init();
    }

    private SnapshotLoad snapshotLoad = new SnapshotLoad();

    private static void init() {
        Options options = new Options();
        options.setCreateIfMissing(true);
        TransactionDBOptions dbOptions = new TransactionDBOptions();
        try {
            rocksDB = TransactionDB.open(options, dbOptions, PropertiesUtil.getString("data"));
        } catch (RocksDBException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public MachineResult execute(LogEntry logEntry) throws Exception {
        OptionEnum optionEnum = OptionEnum.getByCode(logEntry.getOption());
        if (optionEnum == OptionEnum.UP) {
            if (logEntry.getTerm() == PersistentStateModel.getModel().getCurrentTerm()) {
                RaftServerData.leaderUp();
            }
            return new MachineResult(logEntry.getId(), OperationState.SUCCESS, null);
        }
        if (optionEnum == null) {
            return new MachineResult(logEntry.getId(), OperationState.FAIL, ("不支持的类型" + logEntry.getOption()).getBytes(StandardCharsets.UTF_8));
        }
        MachineResult result = new MachineResult();
        result.setId(logEntry.getId());
        result.setState(OperationState.SUCCESS);
        Transaction transaction = rocksDB.beginTransaction(new WriteOptions());
        try {
            if (isExec(logEntry.getId().getBytes(StandardCharsets.UTF_8), transaction)) {
                log.info("命令已执行 :" + logEntry.getIndex());
                throw new RaftException("命令已执行");
            }
            switch (optionEnum) {
                case SET:
                    //修改
                    set(logEntry.getKey(), logEntry.getVal(), transaction);
                    result.setData(DataConstants.TRUE);
                    break;
                case DEL:
                    //删除
                    delete(logEntry.getKey(), transaction);
                    break;
                case RESET_INCR:
                case RESET_DECR: {
                    //比较旧值，一致则重置再操作，不一致则在当前基础上操作
                    byte[] current = get(logEntry.getKey(), transaction);
                    DefaultValue value;
                    CompareValue compareValue = compareValueSerializer.deserialize(logEntry.getVal(), new CompareValue());
                    if (Arrays.equals(compareValue.getCompare(), current)) {
                        //和旧值一致，说明期间没有被修改，直接清空再操作
                        value = calculation(optionEnum, null, compareValue.getUpdate());
                    } else {
                        //不一致，说明已经被修改，则在被修改的基础上操作
                        value = calculation(optionEnum, current, compareValue.getUpdate());
                    }
                    result.setData(value.getValue());
                    set(logEntry.getKey(), cmdValueSerializer.serialize(value), transaction);
                }
                break;
                case INCR:
                case DECR: {
                    //自增/自减
                    byte[] current = get(logEntry.getKey(), transaction);
                    DefaultValue value = calculation(optionEnum, current, logEntry.getVal());
                    result.setData(value.getValue());
                    set(logEntry.getKey(), cmdValueSerializer.serialize(value), transaction);
                }
                break;
                case EXPIRE: {
                    //设置过期时间
                    byte[] current = get(logEntry.getKey(), transaction);
                    if (current != null) {
                        DefaultValue currentValue = cmdValueSerializer.deserialize(current, new DefaultValue());
                        DefaultValue update = cmdValueSerializer.deserialize(logEntry.getVal(), new DefaultValue());
                        currentValue.setExpireTime(update.getExpireTime());
                        result.setData(DataConstants.TRUE);
                        set(logEntry.getKey(), cmdValueSerializer.serialize(currentValue), transaction);
                    } else {
                        result.setData(DataConstants.FALSE);
                    }

                }
                break;
                case COMPARE_AND_DEL: {//比较再删除,用于系统删除过期key
                    byte[] current = get(logEntry.getKey(), transaction);
                    if (Arrays.equals(current, logEntry.getVal())) {
                        delete(logEntry.getKey(), transaction);
                    }
                }
                break;
                case SET_IF_ABSENT: {
                    byte[] current = get(logEntry.getKey(), transaction);
                    CompareValue compareValue = compareValueSerializer.deserialize(logEntry.getVal(), new CompareValue());
                    if (current == null || Arrays.equals(current, compareValue.getCompare())) {
                        //不存在 或者值没变
                        set(logEntry.getKey(), compareValue.getUpdate(), transaction);
                        result.setData(DataConstants.TRUE);
                    } else {
                        //值存在或者改变了，都不能再操作
                        result.setData(DataConstants.FALSE);
                    }
                }
                break;
                case SET_IF_PRESENT: {
                    if (get(logEntry.getKey(), transaction) == null) {
                        result.setData(DataConstants.FALSE);
                    } else {
                        set(logEntry.getKey(), logEntry.getVal(), transaction);
                        result.setData(DataConstants.TRUE);
                    }
                }
                break;
                default:
            }
            transaction.commit();
            return result;
        } catch (RaftException e) {
            transaction.rollback();
            result.setState(OperationState.FAIL);
            result.setData(e.getMessage().getBytes(StandardCharsets.UTF_8));
            return result;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    private DefaultValue calculation(OptionEnum optionEnum, byte[] oldSrc, byte[] change) {
        DefaultValue oldValue;
        byte[] old;
        byte[] val = null;
        if (oldSrc != null) {
            oldValue = cmdValueSerializer.deserialize(oldSrc, new DefaultValue());
        } else {
            oldValue = new DefaultValue(null, -1);
        }
        old = oldValue.getValue();
        //改变的值
        DefaultValue cal = change == null ? null : cmdValueSerializer.deserialize(change, new DefaultValue());
        if (cal != null) {
            val = cal.getValue();
            oldValue.setExpireTime(cal.getExpireTime());
        }

        byte[] bytes;
        long temp;
        if (val != null) {
            try {
                temp = Long.parseLong(serializer.deserialize(val, String.class));
            } catch (Exception e) {
                throw new RaftException("不是数字");
            }
        } else {
            temp = 1;
        }
        if (old != null) {
            try {
                String str = serializer.deserialize(old, String.class);
                String num = Long.toString(calculation(optionEnum, Long.parseLong(str), temp));
                bytes = serializer.serialize(num);
            } catch (Exception e) {
                throw new RaftException("不是数字");
            }
        } else {
            if (val == null) {
                bytes = optionEnum == OptionEnum.INCR || optionEnum == OptionEnum.RESET_INCR ? DataConstants.POSITIVE_ONE : DataConstants.NEGATIVE_ONE;
            } else {
                bytes = serializer.serialize(Long.toString(calculation(optionEnum, 0, temp)));
            }
        }
        oldValue.setValue(bytes);
        return oldValue;
    }

    private long calculation(OptionEnum optionEnum, long old, long val) {
        return optionEnum == OptionEnum.INCR || optionEnum == OptionEnum.RESET_INCR ? old + val : old - val;
    }

    @Override
    public void execute(List<KvEntity> data, int term, long index) throws Exception {
        for (KvEntity kvEntity : data) {
            rocksDB.put(kvEntity.getKey(), kvEntity.getVal());
            snapshotLoad.set(kvEntity.getKey(), kvEntity.getVal(), term, index);
        }
    }

    private void set(byte[] key, byte[] value, Transaction transaction) throws Exception {
        transaction.put(addPrefix(STRING, key), value);
    }

    private void delete(byte[] key, Transaction transaction) throws Exception {
        transaction.delete(addPrefix(STRING, key));
    }

    /**
     * 判断命令是否执行
     *
     * @param id          命令id
     * @param transaction 操作类
     * @return true/false
     * @throws RocksDBException 操作异常
     */
    private boolean isExec(byte[] id, Transaction transaction) throws RocksDBException {
        byte[] key = addPrefix(SERIAL_NUMBER_PRE, id);
        byte[] rs = transaction.get(new ReadOptions(), key);
        if (rs == null) {
            transaction.put(key, SERIAL_NUMBER_VAL);
            return false;
        }
        return true;
    }

    @Override
    public byte[] get(byte[] key) throws Exception {
        return rocksDB.get(addPrefix(STRING, key));
    }

    public byte[] get(byte[] key, Transaction transaction) throws Exception {
        return transaction.get(new ReadOptions(), addPrefix(STRING, key));
    }

    @Override
    public void reset() throws Exception {
        RocksIterator iterator = rocksDB.newIterator();
        iterator.seekToFirst();
        byte[] first = iterator.key();
        iterator.seekToLast();
        byte[] last = iterator.key();
        rocksDB.deleteRange(first, last);
        rocksDB.delete(last);
        snapshotLoad.reset();
    }

    private byte[] addPrefix(byte[] prefix, byte[] keyNext) {
        byte[] key = Arrays.copyOf(prefix, prefix.length + keyNext.length);
        System.arraycopy(keyNext, 0, key, prefix.length, keyNext.length);
        return key;
    }


    @Override
    public void save(LogEntry logEntry) throws Exception {
        OptionEnum optionEnum = OptionEnum.getByCode(logEntry.getOption());
        if (optionEnum == null) {
            snapshotLoad.updateTermAndIndex(logEntry.getTerm(), logEntry.getIndex());
            return;
        }
        if (optionEnum == OptionEnum.UP) {
            snapshotLoad.updateTermAndIndex(logEntry.getTerm(), logEntry.getIndex());
        }
        byte[] idKey = addPrefix(SERIAL_NUMBER_PRE, logEntry.getId().getBytes(StandardCharsets.UTF_8));
        byte[] rs = rocksDB.get(idKey);
        if (rs == null) {
            //没有序列号说明该指令已经执行过了，不需要重复执行
            log.info("已生成快照:{}", logEntry.getIndex());
            return;
        }
        long lastIndex = snapshotLoad.getIndex();
        if (lastIndex >= logEntry.getIndex()) {
            //最终索引大于当前的日志索引，说明该日志已经执行过了
            rocksDB.delete(idKey);
            return;
        }
        try {
            switch (optionEnum) {
                case SET:
                    snapshotLoad.set(addPrefix(STRING, logEntry.getKey()), logEntry.getVal(), logEntry.getTerm(), logEntry.getIndex());
                    break;
                case DEL:
                    snapshotLoad.del(addPrefix(STRING, logEntry.getKey()), logEntry.getTerm(), logEntry.getIndex());
                    break;
                case EXPIRE: {
                    //设置过期时间
                    byte[] key = addPrefix(STRING, logEntry.getKey());
                    byte[] current = snapshotLoad.get(key);
                    if (current != null) {
                        DefaultValue currentValue = cmdValueSerializer.deserialize(current, new DefaultValue());
                        DefaultValue update = cmdValueSerializer.deserialize(logEntry.getVal(), new DefaultValue());
                        currentValue.setExpireTime(update.getExpireTime());
                        snapshotLoad.set(key, cmdValueSerializer.serialize(currentValue), logEntry.getTerm(), logEntry.getIndex());
                    } else {
                        snapshotLoad.updateTermAndIndex(logEntry.getTerm(), logEntry.getIndex());
                    }

                }
                case COMPARE_AND_DEL: {//比较再删除,用于系统删除过期key
                    byte[] key = addPrefix(STRING, logEntry.getKey());
                    byte[] current = snapshotLoad.get(key);
                    if (Arrays.equals(current, logEntry.getVal())) {
                        snapshotLoad.del(key, logEntry.getTerm(), logEntry.getIndex());
                    } else {
                        snapshotLoad.updateTermAndIndex(logEntry.getTerm(), logEntry.getIndex());
                    }
                }
                break;
                case RESET_INCR:
                case RESET_DECR: {
                    byte[] key = addPrefix(STRING, logEntry.getKey());
                    //比较旧值，一致则重置再操作，不一致则在当前基础上操作
                    byte[] current = snapshotLoad.get(key);
                    DefaultValue value;
                    CompareValue compareValue = compareValueSerializer.deserialize(logEntry.getVal(), new CompareValue());
                    if (Arrays.equals(compareValue.getCompare(), current)) {
                        //和旧值一致，说明期间没有被修改，直接清空再操作
                        value = calculation(optionEnum, null, compareValue.getUpdate());
                    } else {
                        //不一致，说明已经被修改，则在被修改的基础上操作
                        value = calculation(optionEnum, current, compareValue.getUpdate());
                    }
                    snapshotLoad.set(key, cmdValueSerializer.serialize(value), logEntry.getTerm(), logEntry.getIndex());
                }
                break;
                case INCR:
                case DECR: {
                    byte[] key = addPrefix(STRING, logEntry.getKey());
                    byte[] old = snapshotLoad.get(key);
                    DefaultValue value = calculation(optionEnum, old, logEntry.getVal());
                    snapshotLoad.set(key, cmdValueSerializer.serialize(value), logEntry.getTerm(), logEntry.getIndex());
                }
                break;
                case SET_IF_ABSENT: {
                    byte[] key = addPrefix(STRING, logEntry.getKey());
                    byte[] current = snapshotLoad.get(key);
                    CompareValue compareValue = compareValueSerializer.deserialize(logEntry.getVal(), new CompareValue());
                    if (current == null || Arrays.equals(current, compareValue.getCompare())) {
                        //不存在 或者值没变
                        snapshotLoad.set(key, compareValue.getUpdate(), logEntry.getTerm(), logEntry.getIndex());
                    } else {
                        snapshotLoad.updateTermAndIndex(logEntry.getTerm(), logEntry.getIndex());
                    }
                }
                break;
                case SET_IF_PRESENT: {
                    byte[] key = addPrefix(STRING, logEntry.getKey());
                    if (snapshotLoad.get(key) != null) {
                        snapshotLoad.set(key, logEntry.getVal(), logEntry.getTerm(), logEntry.getIndex());
                    } else {
                        snapshotLoad.updateTermAndIndex(logEntry.getTerm(), logEntry.getIndex());
                    }
                }
                break;
                default:
            }
        } catch (RaftException e) {
            log.info("快照执行异常", e);
            snapshotLoad.updateTermAndIndex(logEntry.getTerm(), logEntry.getIndex());
        }
        rocksDB.delete(idKey);
    }

    @Override
    public long snapshotLastIndex() throws Exception {
        return snapshotLoad.getIndex();
    }

    @Override
    public int snapshotLastTerm() throws Exception {
        return snapshotLoad.getTerm();
    }

    @Override
    public List<KvEntity> getSnapshotData(byte[] lastKey, int maxLen) {
        return snapshotLoad.get(lastKey, maxLen);
    }
}
