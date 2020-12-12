package org.top.models;

import lombok.Getter;
import lombok.Setter;

/**
 * @author lubeilin
 * @date 2020/11/17
 */
@Getter
@Setter
public class LogEntry {
    private long index;
    private int term;
    private byte[] id;
    private String option;
    private byte[] key;
    private byte[] val;

    @Override
    public String toString() {
        return "LogEntry{" +
                "index=" + index +
                '}';
    }
}
