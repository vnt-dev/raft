package org.top.utils;

import org.top.rpc.codec.ProtoBufSerializer;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
public class DataConstants {
    public static final byte[] TRUE;
    public static final byte[] FALSE = null;
    public static final byte[] POSITIVE_ONE;
    public static final byte[] NEGATIVE_ONE;

    static {
        ProtoBufSerializer<Boolean> booleanProtoBufSerializer = new ProtoBufSerializer<>();
        ProtoBufSerializer<String> stringProtoBufSerializer = new ProtoBufSerializer<>();
        TRUE = booleanProtoBufSerializer.serialize(true);
        POSITIVE_ONE = stringProtoBufSerializer.serialize("1");
        NEGATIVE_ONE = stringProtoBufSerializer.serialize("-1");
    }
}
