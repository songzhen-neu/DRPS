package store;

import Util.FileUtil;
import Util.RandomUtil;
import Util.TypeExchangeUtil;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import lombok.Data;
import lombok.Synchronized;
import net.IntListMessage;
import net.KeyValueListMessage;
import net.SListMessage;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: simplePsForModelPartition
 * @description: 磁盘的KVStore
 * @author: SongZhen
 * @create: 2018-12-07 16:14
 */

@Data
public class KVStoreForLevelDB {
    DB db;
    AtomicLong curIndexOfSparseDim=new AtomicLong(0);
    public void init(String path) throws IOException {
        FileUtil.deleteFile(new File(path+"db/"));
        db= Iq80DBFactory.factory.open(new File(path,"db"),new Options().createIfMissing(true));
    }

    @Synchronized
    public Map<String,Long> getIndex(SListMessage sListMessage)throws IOException,ClassNotFoundException{
        Map<String,Long> map=new HashMap<String, Long>();
        for(int i=0;i<sListMessage.getSize();i++){
            String key=sListMessage.getList(i);
            byte[] mapKey=db.get(("catDimMap"+key).getBytes());
            if (mapKey!=null){
                map.put(key,(Long) TypeExchangeUtil.toObject(mapKey));
            }
            else {
                db.put(("catDimMap"+key).getBytes(),TypeExchangeUtil.toByteArray(curIndexOfSparseDim.longValue()));
                map.put(key,curIndexOfSparseDim.longValue());
                curIndexOfSparseDim.incrementAndGet();

            }
        }
        return map;
    }

    public void initParams() throws IOException{
        for(int i=0;i<Context.sparseDimSize;i++){
            if(i%Context.serverNum==ServerContext.serverId){
                db.put(("catParam"+i).getBytes(),TypeExchangeUtil.toByteArray(RandomUtil.getRandomValue(-0.1f,0.1f)));
                System.out.println("params:"+i);
            }
        }
    }


}