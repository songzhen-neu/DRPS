package TestClass;

import Util.CurrentTimeUtil;
import Util.TypeExchangeUtil;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-03-31 19:56
 */
public class TestSeekTime {
    public static long num=3000000L;
    public static int testnum=3000000;

    public static void write() throws IOException {
        String filePath="data/";
        DB db = Iq80DBFactory.factory.open((new File(filePath,"db/")),new Options().createIfMissing(true));
        byte[][] bytesTest=new byte[testnum][];
        for(int i=0;i<num;i++) {
            byte[] bytes = ("" + i).getBytes();
            byte[] bytesValue = "a".getBytes();
            db.put(bytes, bytesValue);
            bytesTest[i] = bytesValue;
        }
        byte[] bytes=new byte[testnum];
        db.put("test".getBytes(),TypeExchangeUtil.toByteArray(bytesTest));
        db.close();

    }

    public static void seek() throws IOException {
        String filePath = "data/";
        DB db = Iq80DBFactory.factory.open((new File(filePath, "db/")), new Options().createIfMissing(true));
        for (int i = 0; i < 5; i++) {

            CurrentTimeUtil.setStartTime();
            db.get("test".getBytes());
            CurrentTimeUtil.setEndTime();
            // 顺序读取所有数据时间
            CurrentTimeUtil.showExecuteTime(i+"-seq:");

            CurrentTimeUtil.setStartTime();
            for(int j=0;j<testnum;j++){
                // 随机读
                byte[] getValuebyte=db.get((""+j).getBytes());
            }
            CurrentTimeUtil.setEndTime();
            // 随机读取所有数据时间
            CurrentTimeUtil.showExecuteTime(i+"-random:");


        }
        db.close();
    }
}