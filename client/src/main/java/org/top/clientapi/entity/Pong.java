package org.top.clientapi.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.top.rpc.codec.BaseMessage;
import org.top.rpc.entity.Node;

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
