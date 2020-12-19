package org.top.rpc.codec;

import lombok.Data;
import lombok.ToString;

/**
 * 消息数据传输封装
 *
 * @author lubeilin
 * @date 2020/11/3
 */
@Data
@ToString
public class ObjectData {
    private transient BaseMessage message;
    private String className;
    private byte[] bytes;

    public ObjectData() {
    }

    public ObjectData(BaseMessage message) {
        this.message = message;
    }

    public byte[] serialize() {
        className = message.getClass().getName();
        bytes = new ProtoBufSerializer<BaseMessage>().serialize(message);
        return new ProtoBufSerializer<ObjectData>().serialize(this);
    }

    public ObjectData deserialize(byte[] bytes) throws ClassNotFoundException {
        ObjectData objectData = new ProtoBufSerializer<ObjectData>().deserialize(bytes, this);
        Class<?> clazz = Class.forName(objectData.className);
        @SuppressWarnings("unchecked")
        BaseMessage message = (BaseMessage) new ProtoBufSerializer<>().deserialize(objectData.getBytes(), (Class<Object>) clazz);
        objectData.setMessage(message);
        return objectData;
    }
}
