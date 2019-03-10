package store;

import Util.FileUtil;
import Util.MessageDataTransUtil;
import Util.RandomUtil;
import Util.TypeExchangeUtil;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import dataStructure.parameter.Param;
import lombok.Data;
import lombok.Synchronized;
import net.*;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
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
    AtomicLong curIndexOfSparseDim = new AtomicLong(0);
    private float[] featureParams = new float[Context.featureSize];
    Map<Integer, Float> timeCostMap = new ConcurrentHashMap<Integer, Float>();
    AtomicInteger minTimeCostI = new AtomicInteger(-1);
    Set[] vSet;
    // listSet其实是一个存储各个server中存储了哪些参数（有本地参数划分）的数据结构
    public static List<Set>[] ls_partitionedVSet = new ArrayList[Context.serverNum];


    public void init(String path) throws IOException {
        FileUtil.deleteFile(new File(path + "db/"));
        db = Iq80DBFactory.factory.open(new File(path, "db"), new Options().createIfMissing(true));
    }

    @Synchronized
    public Map<String, Long> getIndex(SListMessage sListMessage) throws IOException, ClassNotFoundException {
        Map<String, Long> map = new HashMap<String, Long>();
        for (int i = 0; i < sListMessage.getSize(); i++) {
            String key = sListMessage.getList(i);
            byte[] mapKey = db.get(("catDimMap" + key).getBytes());
            if (mapKey != null) {
                map.put(key, (Long) TypeExchangeUtil.toObject(mapKey));
            } else {
                db.put(("catDimMap" + key).getBytes(), TypeExchangeUtil.toByteArray(curIndexOfSparseDim.longValue()));
                map.put(key, curIndexOfSparseDim.longValue());
                curIndexOfSparseDim.incrementAndGet();

            }
        }
        return map;
    }

    public void initParams(Long sparseDimSize, Set<Long>[] vSet, List<Set> ls_params) throws IOException {
        System.out.println("sparseDimSize:" + sparseDimSize);

        // 这是按照最初的取余进行分配的
        for (long i = 0; i < sparseDimSize; i++) {
            if (i % Context.serverNum == ServerContext.serverId) {
                db.put(("catParam" + i).getBytes(), TypeExchangeUtil.toByteArray(RandomUtil.getRandomValue(-0.1f, 0.1f)));
                System.out.println("params:" + i);
            }
        }

        // 这里不能简单的key，value分配了，因为已经进行磁盘划分，那么就有集合的形式了
        for (Set set : ls_params) {
            Set<Param> paramSet = new HashSet<Param>();
            for (Object l : set) {
                Param param = new Param("catParam" + l, RandomUtil.getRandomValue(-0.1f, 0.1f));
                paramSet.add(param);
            }
            db.put(("catParamSet" + ls_params.indexOf(set)).getBytes(), TypeExchangeUtil.toByteArray(paramSet));
            // 查询的时候通过ls_params查询，反正是set集合，那么就用contains进行查询
        }
//        for(long i:vSet[ServerContext.serverId]){
//            db.put(("catParam"+i).getBytes(),TypeExchangeUtil.toByteArray(RandomUtil.getRandomValue(-0.1f,0.1f)));
//            System.out.println("params:"+i);
//        }

        // 再把放在别的server里vset删除
        for (int i = 0; i < vSet.length; i++) {
            if (i != ServerContext.serverId) {
                for (long l : vSet[i]) {
                    if (db.get(("catParam" + l).getBytes()) != null) {
                        db.delete(("catParam" + l).getBytes());
                    }
                }
            }
        }


        if (Context.masterId == WorkerContext.workerId) {
            for (int i = 0; i < featureParams.length; i++) {
                featureParams[i] = RandomUtil.getRandomValue(-0.1f, 0.1f);
            }
        }

    }

    @Synchronized
    public SFKVListMessage getNeededParams(Set<String> set) throws ClassNotFoundException, IOException {
        DB db = ServerContext.kvStoreForLevelDB.getDb();

        SFKVListMessage.Builder sfkvlistMessage = SFKVListMessage.newBuilder();
        Map<String, Float> map = new HashMap<String, Float>();
        List<Set> paramKeySetList = ls_partitionedVSet[ServerContext.serverId];
        long index = -1;
        for (String key : set) {
            for (Set<Long> paramKeySet : paramKeySetList) {
                if (paramKeySet.contains(key)) {
                    index = paramKeySetList.indexOf(paramKeySet);
                }
            }
            if (index == -1) {
                Float f = (Float) TypeExchangeUtil.toObject(db.get(key.getBytes()));
                map.put(key, f);
            } else {
                Set<Param> paramSet = (Set<Param>) TypeExchangeUtil.toObject(db.get(("catParamSet" + index).getBytes()));
                for (Param param : paramSet) {
                    if (param.key.equals(key)) {
                        Float f = param.value;
                        map.put(key, f);
                    }
                }
            }
            index = -1;
        }
        if (ServerContext.serverId == Context.masterId) {
            for (int i = 0; i < featureParams.length; i++) {
                map.put("featParam" + i, featureParams[i]);
            }
        }
        return MessageDataTransUtil.Map_2_SFKVListMessage(map);
    }

    @Synchronized
    public void updateParams(Map<String, Float> map) {
        /**
        *@Description: 在这个函数中会调用updateKVStore，这是个大的过程，而updateKVStore仅仅是更新那一步
        *@Param: [map]
        *@return: void
        *@Author: SongZhen
        *@date: 下午4:03 19-3-10
        */
        try {
            for (String index : map.keySet()) {
//                System.out.println(index);
                if (index.contains("featParam")) {
                    String[] split = index.split("m");
                    featureParams[Integer.parseInt(split[1])] += map.get(index);

                } else {
                    updateKVStore(index, map);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void updateKVStore(String key, Map<String,Float> map) throws IOException,ClassNotFoundException {
        // 需要先判断是不是划分里的参数，如果是那么按照paramSet的方式更新，如果不是直接按照catParam的方式更新
        List<Set> ls_params = ls_partitionedVSet[ServerContext.serverId];
        long index = -1;

        for (Set<Long> paramKeySet : ls_params) {
            if (paramKeySet.contains(key)) {
                index = ls_params.indexOf(paramKeySet);
                break;
            }
        }
        if (index == -1) {
            float f=(Float) TypeExchangeUtil.toObject(db.get(TypeExchangeUtil.toByteArray(key)));
            db.delete(TypeExchangeUtil.toByteArray(key));
            db.put(TypeExchangeUtil.toByteArray(key), TypeExchangeUtil.toByteArray(f+map.get(key)));
        } else {
            Set<Param> paramSet = (Set<Param>) TypeExchangeUtil.toObject(db.get(("catParamSet" + index).getBytes()));
            for (Param param : paramSet) {
                if (param.key.equals(key)) {
                    param.value+=map.get(key);
                    db.delete(("catParamSet"+index).getBytes());
                    db.put(("catParamSet"+index).getBytes(), TypeExchangeUtil.toByteArray(paramSet));
                    break;
                }
            }


        }



    }


}