package org.top.clientapi.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.top.rpc.Node;
import org.top.rpc.codec.BaseMessage;

/**
 * @author lubeilin
 * @date 2020/11/27
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Pong extends BaseMessage {
    private Node redirect;
}
