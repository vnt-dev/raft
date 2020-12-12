package org.top.rpc;

import org.apache.commons.lang3.StringUtils;
import org.top.rpc.utils.PropertiesUtil;

import java.util.LinkedHashSet;
import java.util.function.Consumer;

/**
 * @author lubeilin
 * @date 2020/11/9
 */
public class NodeGroup {
    private LinkedHashSet<Node> nodeSet = new LinkedHashSet<>();
    public static final Node MYSELF = new Node(PropertiesUtil.getString("ip"), PropertiesUtil.getInt("port"));
    private static NodeGroup nodeGroup = new NodeGroup();

    public static NodeGroup getNodeGroup() {
        return nodeGroup;
    }

    public Node getNode(Node node) {
        for (Node node1 : nodeSet) {
            if (node1.equals(node)) {
                return node1;
            }
        }
        return null;
    }

    public NodeGroup() {
        String nodesStr = PropertiesUtil.getString("nodes");
        if (StringUtils.isNotEmpty(nodesStr)) {
            String[] nodeStrArr = nodesStr.split(",");
            for (String nodeStr : nodeStrArr) {
                String[] ipAndPort = nodeStr.split(":");
                Node node = new Node(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
                nodeSet.add(node);
            }
        }
        nodeSet.remove(MYSELF);
    }

    public void parallelForEach(Consumer<Node> action) {
        nodeSet.parallelStream().forEach(action);
    }

    public void forEach(Consumer<Node> action) {
        nodeSet.forEach(action);
    }

    public int size() {
        return nodeSet.size();
    }

    public int majority() {
        return (nodeSet.size() + 1) / 2 + 1;
    }
}
