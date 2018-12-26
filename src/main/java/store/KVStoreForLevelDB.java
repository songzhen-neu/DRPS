package store;

import Util.FileUtil;
import Util.MessageDataTransUtil;
import Util.RandomUtil;
import Util.TypeExchangeUtil;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import lombok.Data;
import lombok.Synchronized;
import net.*;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private float[] featureParams=new float[Context.featureSize];
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
        if(Context.masterId==WorkerContext.workerId){
            for(int i=0;i<featureParams.length;i++){
                featureParams[i]=RandomUtil.getRandomValue(-0.1f,0.1f);
            }
        }

    }

    @Synchronized
    public SFKVListMessage getNeededParams(Set<String> set) throws ClassNotFoundException,IOException{
        DB db=ServerContext.kvStoreForLevelDB.getDb();
        SFKVListMessage.Builder sfkvlistMessage=SFKVListMessage.newBuilder();
        Map<String,Float> map=new HashMap<String, Float>();
        for(String key:set){
            Float f=(Float) TypeExchangeUtil.toObject(db.get(key.getBytes()));
            map.put(key,f);
        }
        if(ServerContext.serverId==Context.masterId){
            for(int i=0;i<featureParams.length;i++){
                map.put("featParam"+i,featureParams[i]);
            }
        }
        return MessageDataTransUtil.Map_2_SFKVListMessage(map);
    }

    @Synchronized
    public void updateParams(Map<String,Float> map) {
        try{
            for(String index:map.keySet()){
//                System.out.println(index);
                if(index.contains("featParam")){
                    String[] split=index.split("m");
                    featureParams[Integer.parseInt(split[1])]+=map.get(index);

                }else {
                    float param=(Float) TypeExchangeUtil.toObject(db.get(index.getBytes()));
                    param+=map.get(index);
                    updateKVStore(index,param);
                }

            }
        }catch (IOException e){
            e.printStackTrace();
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }

    }

    public void updateKVStore(Object key,Object value) throws IOException{
        db.delete(TypeExchangeUtil.toByteArray(key));
        db.put(TypeExchangeUtil.toByteArray(key),TypeExchangeUtil.toByteArray(value));
    }


}