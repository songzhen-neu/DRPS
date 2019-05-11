package TestClass;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-10 15:00
 */
public class TestDB1 {
    public static void main(String[] args) throws IOException {
        DB db = Iq80DBFactory.factory.open(new File("data/leveldbForWorker"), new Options().createIfMissing(true));
        db.put("bbb".getBytes(), "asdasdas".getBytes());

        db.get("bbb".getBytes());


//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        System.out.println("aaa");
        db.close();



    }
}