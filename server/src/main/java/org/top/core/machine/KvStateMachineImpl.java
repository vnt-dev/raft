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
                    set(logEntry.getKey(), logEntry.getVal(), transaction);
                    result.setData(DataConstants.TRUE);
                    break;
                case DEL:
                    delete(logEntry.getKey(), transaction);
                    break;
                case RESET_INCR:
                case RESET_DECR:
                case INCR:
                case DECR: {
                    byte[] old = optionEnum == OptionEnum.RESET_INCR || optionEnum == OptionEnum.RESET_DECR ? null : get(logEntry.getKey(), transaction);
                    DefaultValue value = calculation(optionEnum, old, logEntry.getVal());
                    result.setData(value.getValue());
                    set(logEntry.getKey(), cmdValueSerializer.serialize(value), transaction);
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
                bytes = optionEnum == OptionEnum.INCR ? DataConstants.POSITIVE_ONE : DataConstants.NEGATIVE_ONE;
            } else {
                bytes = serializer.serialize(Long.toString(calculation(optionEnum, 0, temp)));
            }
        }
        oldValue.setValue(bytes);
        return oldValue;
    }

    private long calculation(OptionEnum optionEnum, long old, long val) {
        return optionEnum == OptionEnum.INCR ? old + val : old - val;
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
            return;
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
        switch (optionEnum) {
            case SET:
                snapshotLoad.set(addPrefix(STRING, logEntry.getKey()), logEntry.getVal(), logEntry.getTerm(), logEntry.getIndex());
                break;
            case DEL:
                snapshotLoad.del(addPrefix(STRING, logEntry.getKey()), logEntry.getTerm(), logEntry.getIndex());
                break;
            case RESET_INCR:
            case RESET_DECR:
            case INCR:
            case DECR:
                try {
                    byte[] old = optionEnum == OptionEnum.RESET_INCR || optionEnum == OptionEnum.RESET_DECR ? null : snapshotLoad.get(logEntry.getKey());
                    DefaultValue value = calculation(optionEnum, old, logEntry.getVal());
                    snapshotLoad.set(addPrefix(STRING, logEntry.getKey()), cmdValueSerializer.serialize(value), logEntry.getTerm(), logEntry.getIndex());
                } catch (RaftException ignored) {

                }
                break;
            default:
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
