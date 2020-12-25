package org.top.exception;

/**
 * @author lubeilin
 * @date 2020/12/22
 */
public class RaftInitException extends RuntimeException {
    public RaftInitException(String message) {
        super(message);
    }
}
