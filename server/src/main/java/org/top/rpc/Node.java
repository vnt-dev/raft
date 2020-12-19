package org.top.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 节点
 *
 * @author lubeilin
 * @date 2020/11/3
 */
@Data
@AllArgsConstructor
public class Node implements Serializable {
    private static final long serialVersionUID = -4525615343545546150L;
    private final String ip;
    private final int port;

    public Node(String host) {
        String[] hs = host.split(":");
        ip = hs[0];
        port = Integer.parseInt(hs[1]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Node)) {
            return false;
        }
        Node node = (Node) o;
        return getPort() == node.getPort() &&
                Objects.equals(getIp(), node.getIp());
    }

    @Override
    public String toString() {
        return ip + ":" + port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIp(), getPort());
    }
}
