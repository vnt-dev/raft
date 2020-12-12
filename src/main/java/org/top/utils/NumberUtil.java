package org.top.utils;

import java.io.*;

/**
 * @author lubeilin
 * @date 2020/12/2
 */
public class NumberUtil {
    public static byte[] toBytes(int index) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bao);
        dos.writeInt(index);
        return bao.toByteArray();
    }

    public static byte[] toBytes(long index) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bao);
        dos.writeLong(index);
        return bao.toByteArray();
    }

    public static int toInt(byte[] bytes) throws IOException {
        ByteArrayInputStream bai = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bai);
        return dis.readInt();
    }

    public static long toLong(byte[] bytes) throws IOException {
        ByteArrayInputStream bai = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bai);
        return dis.readLong();
    }
}
