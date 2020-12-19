package org.top.core;


import org.rocksdb.Transaction;
import org.top.models.PersistentStateModel;

/**
 * @author lubeilin
 * @date 2020/11/17
 */
public class FollowerConvert {
    /**
     * 转换为follower： 设置相关变量
     *
     * @param term 需要转换为的term
     */
    public static boolean convertFollower(int term) throws Exception {
        PersistentStateModel model = PersistentStateModel.getModel();
        if (model.getCurrentTerm() < term) {
            Transaction transaction = model.begin();
            RaftServerData.lock.lock();
            try {
                if (model.getCurrentTerm() < term) {
                    RaftServerData.serverStateEnum = ServerStateEnum.FOLLOWER;
                    RaftServerData.heartbeatTime = System.currentTimeMillis();
                    model.setCurrentTerm(term, transaction);
                    model.setVotedFor(null, transaction);
                    transaction.commit();
                    ClientNum.convertFollower();
                    return true;
                }
            } catch (Exception e) {
                transaction.rollback();
                throw e;
            } finally {
                RaftServerData.lock.unlock();
            }
        }
        return false;
    }
}
