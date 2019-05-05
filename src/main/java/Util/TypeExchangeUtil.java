package Util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

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

    public static List<Set> copyListSet(List<Set> ls){
        /**
        *@Description: 新建一个和ls内容完全一直的ls对象
        *@Param: [ls]
        *@return: java.util.List<java.util.Set>
        *@Author: SongZhen
        *@date: 下午2:00 19-3-12
        */
        List<Set> ls_new=new ArrayList<Set>();
        for(Set set:ls){
            Set<Long> set_new=new HashSet<Long>();
            for(Object l: set){
                set_new.add((Long) l);
            }
            ls_new.add(set_new);
        }

        return ls_new;
    }


}
