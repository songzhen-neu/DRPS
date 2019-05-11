package TestClass;

import Util.CurrentTimeUtil;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-10 14:59
 */
public class TestDB {
    public static void main(String[] args) throws IOException {
        DB db = Iq80DBFactory.factory.open(new File("data/leveldbForWorker"), new Options().createIfMissing(true));
        db.put("aaa".getBytes(), "asdasdas".getBytes());
        db.get("aaa".getBytes());
        db.close();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        db = Iq80DBFactory.factory.open(new File("data/leveldbForWorker"), new Options().createIfMissing(true));
        db.get("aaa".getBytes());

        System.out.println("aaa");
        db.close();


        // 下面测试开关db的时间
        // 数据库开关大概需要10ms
        CurrentTimeUtil.setStartTime();
        for(int i=0;i<50;i++){
            db = Iq80DBFactory.factory.open(new File("data/leveldbForWorker"), new Options().createIfMissing(true));
            db.close();
        }
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("leveldb open和close的时间");

    }

}