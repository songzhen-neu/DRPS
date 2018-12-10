package Util;

import java.io.*;

public class TypeExchangeUtil {
    public static byte[] toByteArray(Object object) throws IOException {
        byte[] bytes=null;
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream=new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        bytes=byteArrayOutputStream.toByteArray();
        objectOutputStream.close();
        byteArrayOutputStream.close();
        return  bytes;
    }

    public static Object toObject(byte[] bytes) throws IOException,ClassNotFoundException{
        Object object=null;
        ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream=new ObjectInputStream(byteArrayInputStream);
        object=objectInputStream.readObject();
        objectInputStream.close();
        byteArrayInputStream.close();

        return object;
    }


}
