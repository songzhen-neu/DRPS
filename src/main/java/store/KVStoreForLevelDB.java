package store;

import Util.*;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import dataStructure.SparseMatrix.Matrix;
import dataStructure.parallelismControlModel.IterationTimeTable;
import dataStructure.parallelismControlModel.StrategyChoiceTable;
import dataStructure.parameter.Param;
import dataStructure.parameter.ParamLMF.RowOrColParam;
import io.netty.util.internal.ConcurrentSet;
import lombok.Data;
import lombok.Synchronized;
import net.*;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.ColorUIResource;
import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
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
    // worker在参数向server分流的时候使用的map
    public static Map<Long,Integer> paramAssignedToServerMap;
    // listSet其实是一个存储各个server中存储了哪些参数（有本地参数划分）的数据结构
    public static List<Set>[] ls_partitionedVSet = new ArrayList[Context.serverNum];
    public static List<Set> localParitionVSet = ls_partitionedVSet[ServerContext.serverId];
    public static Map<String, String> catToCatSetMap = new HashMap<String, String>();
    public static Logger logger = LoggerFactory.getLogger(KVStoreForLevelDB.class);
    public static AtomicInteger diskIOCount=new AtomicInteger(0);

//    public static ConcurrentMap<Long,String> catToCatSetMap;


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
                Param param = new Param("p" + i, RandomUtil.getRandomValue(-0.1f, 0.1f));
                db.put(("p" + i).getBytes(), TypeExchangeUtil.toByteArray(param));
//                System.out.println("params:" + i);
            }
        }

        // 还有一部分数据是划分到这台服务器上，但是既不在catParamSet，又不在取余的参数中，而在Vset中


        // 这里不能简单的key，value分配了，因为已经进行磁盘划分，那么就有集合的形式了
        for (Set set : ls_params) {
//            if (set.size() == 1) {
//                // 这里的意思是把ls_params中长度为1的set，按照普通不划分的方法存储
//                for (long l : (Set<Long>) set) {
//                    Param param = new Param("p" + l, RandomUtil.getRandomValue(-0.1f, 0.1f));
//                    db.put(("p" + l).getBytes(), TypeExchangeUtil.toByteArray(param));
//                }
//            } else {
                Set<Param> paramSet = new HashSet<Param>();
                for (Object l : set) {
                    Param param = new Param("p" + l, RandomUtil.getRandomValue(-0.1f, 0.1f));
//                logger.info("catParam"+l);
                    paramSet.add(param);
                }
                db.put(("s" + ls_params.indexOf(set)).getBytes(), TypeExchangeUtil.toByteArray(paramSet));
//            }


        }


//        for(long i:vSet[ServerContext.serverId]){
//            db.put(("catParam"+i).getBytes(),TypeExchangeUtil.toByteArray(RandomUtil.getRandomValue(-0.1f,0.1f)));
//            System.out.println("params:"+i);
//        }

        // 再把放在别的server里vset删除
        for (int i = 0; i < vSet.length; i++) {
            if (i != ServerContext.serverId) {
                for (long l : vSet[i]) {
                    if (db.get(("p" + l).getBytes()) != null) {
                        db.delete(("p" + l).getBytes());
                    }
                }
            }
        }


        if (Context.masterId == ServerContext.serverId) {
            for (int i = 0; i < featureParams.length; i++) {
                featureParams[i] = RandomUtil.getRandomValue(-0.1f, 0.1f);
            }
        }

        localParitionVSet = ls_partitionedVSet[ServerContext.serverId];
        for (Set<Long> set : localParitionVSet) {
            for (Long l : set) {
                catToCatSetMap.put("p" + l, "s" + localParitionVSet.indexOf(set));
            }
        }

    }

    static long totleTimeOfgetParams = 0;

    @Synchronized
    public SFKVListMessage getNeededParams(Set<String> set) throws ClassNotFoundException, IOException {
        CurrentTimeUtil.setStartTime();
        DB db = ServerContext.kvStoreForLevelDB.getDb();

        SFKVListMessage.Builder sfkvlistMessage = SFKVListMessage.newBuilder();
        // map仅存储需要的，而paramMap可能会包括一些catParamSet多余的参数
        Map<String, Float> map = new HashMap<String, Float>();
        List<Set> paramKeySetList = ls_partitionedVSet[ServerContext.serverId];
        long index = -1;
        // 存储需要访问的参数，也就是包含划分块的参数。catParamSet和catParam
        Set<String> needParam = new HashSet<String>();
        // 将需要用到的参数取出来，形成一个map
        Map<String, Float> paramMap = new HashMap<String, Float>();


        // 先转化需要取出来哪些参数
        needParam = getNeedPartitionParam(set);
        diskIOCount.set(diskIOCount.intValue()+needParam.size());


        // 构建参数map

        for (String str : needParam) {
            if (str.indexOf("s") == -1) {
                if (db.get(str.getBytes()) == null) {
                    logger.info("nullstr:" + str);
                }
//                System.out.println("asdasasf:"+str);
                Param p = (Param) TypeExchangeUtil.toObject(db.get(str.getBytes()));

                paramMap.put(p.key, p.value);
            } else {
                Set<Param> temp_catParamSet =  (Set<Param>) TypeExchangeUtil.toObject(db.get(str.getBytes()));
                for (Param param : temp_catParamSet) {
                    paramMap.put(param.key, param.value);
                }
            }
        }



        for (String key : set) {
            map.put(key, paramMap.get(key));
        }
        if (ServerContext.serverId == Context.masterId) {
            for (int i = 0; i < featureParams.length; i++) {
                map.put("f" + i, featureParams[i]);
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
        // 需要在内存中建一个map是从catParam到catSetParam的映射


//        CurrentTimeUtil.setStartTime();
        Set<String> needParam = getNeedPartitionParam(map.keySet());
//        CurrentTimeUtil.setEndTime();
//        CurrentTimeUtil.showExecuteTime("getNeedPartitionParam time:");

        List<Set> localParitionVSet = ls_partitionedVSet[ServerContext.serverId];
        // 先将需要用到的参数读取到内存中,然后更新
        // 要更新的就三种参数，featParam在内存中好更新
        // 还剩catParam和catParamSet，那就建两个数据结构

        // 存储catParam
        Map<String, Float> catParamMap = new HashMap<String, Float>();
        Map<String, Set<Param>> catParamSetMap = new HashMap<String, Set<Param>>();
        Map<String, Float> allCatParamMap = new HashMap<String, Float>();
        Map<String, Set<Param>> updateCatParamSetMap = new HashMap<String, Set<Param>>();
        Map<String, Float> updateCatParamMap = new HashMap<String, Float>();


        for (String index : needParam) {
//                System.out.println(index);
            try {
                if (index.contains("f")) {
                    String[] split = index.split("f");
                    featureParams[Integer.parseInt(split[1])] += map.get(index);

                } else if (index.contains("s")) {
                    if (!catParamSetMap.keySet().contains(index)) {
                        Set<Param> paramSetTemp = (Set<Param>) TypeExchangeUtil.toObject(db.get(index.getBytes()));
                        catParamSetMap.put(index, paramSetTemp);
                        for (Param param : paramSetTemp) {
                            allCatParamMap.put(param.key, param.value);
                        }
                    }
                } else {
                    Param param = (Param) TypeExchangeUtil.toObject(db.get(index.getBytes()));
                    catParamMap.put(index, param.value);
                    allCatParamMap.put(index, param.value);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }


//        // 遍历map对catParamMap和catParamSetMap更新
//        CurrentTimeUtil.setStartTime();
//        for(String index:map.keySet()){
//            boolean isCatParamSetFlag= Boolean.FALSE;
//            // 先遍历找一遍
//            for(Set<String> set:localParitionVSet){
//                if (set.contains(Long.parseLong(index.split("m")[1]))){
//                    Set<Param> params=catParamSetMap.get("catParamSet"+localParitionVSet.indexOf(set));
//                    for(Param param:params){
//                        if(param.key.equals(index)){
//                            param.value+=map.get(index);
//                            isCatParamSetFlag=true;
//                        }
//                    }
//                }
//            }
//
//            if(isCatParamSetFlag==false&&!index.contains("featParam")){
////                System.out.println(index);
//                float f=catParamMap.get(index)+map.get(index);
//                catParamMap.remove(index);
//                catParamMap.put(index,f);
//            }
//            isCatParamSetFlag=false;
//
//
//        }
//        CurrentTimeUtil.setEndTime();
//        CurrentTimeUtil.showExecuteTime("update map time:");




        // 就完全构建试试速度
        // 更新累加值
        for (String index : map.keySet()) {
            if (!index.contains("f")) {
                float f = allCatParamMap.get(index) + map.get(index);
                allCatParamMap.remove(index);
                allCatParamMap.put(index, f);
            }

        }

        // 构造新的catParamSet以及catParam
        for (String index : allCatParamMap.keySet()) {
            if (catToCatSetMap.containsKey(index)) {
                String index_catParamSet = catToCatSetMap.get(index);
                Set<Param> paramSet = updateCatParamSetMap.get(index_catParamSet);
                Param param = new Param(index, allCatParamMap.get(index));
                if (paramSet != null) {
                    paramSet.add(param);
                } else {
                    paramSet = new HashSet<Param>();
                    paramSet.add(param);
                    updateCatParamSetMap.put(index_catParamSet, paramSet);
                }
            } else {
                updateCatParamMap.put(index, allCatParamMap.get(index));
            }
        }




        // 遍历catParamMap和catParamSetMap写入

        try {
            for (String str : updateCatParamMap.keySet()) {
                db.delete(str.getBytes());
                Param param = new Param(str, updateCatParamMap.get(str));
                db.put(str.getBytes(), TypeExchangeUtil.toByteArray(param));
            }

            for (String str : updateCatParamSetMap.keySet()) {
                db.delete(str.getBytes());
                db.put(str.getBytes(), TypeExchangeUtil.toByteArray(updateCatParamSetMap.get(str)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public Set<String> getNeedPartitionParam(Set<String> set) {
        /**
         *@Description: 给定set集合，返回包含索引块的需要参数
         *@Param: [set]
         *@return: void
         *@Author: SongZhen
         *@date: 上午8:37 19-3-11
         */

        Set<String> needParam = new HashSet<String>();
        List<Set> paramKeySetList = ls_partitionedVSet[ServerContext.serverId];
        // 先转化需要取出来哪些参数
        for (String key : set) {
//            long index_forKey = -1;
//            for (Set<Long> paramKeySet : paramKeySetList) {
//                if (paramKeySet.size() > 1) {
//                    // 这里key是string，而paramKeySet是long
//                    if (!key.contains("f")) {
//                        if (paramKeySet.contains((Long.parseLong(key.split("p")[1])))) {
//                            index_forKey = paramKeySetList.indexOf(paramKeySet);
//                        }
//                    }
//
//                }
//            }
//            if (index_forKey == -1) {
//                needParam.add(key);
//            } else {
//                needParam.add("s" + index_forKey);
//                index_forKey = -1;
//            }
            if(catToCatSetMap.keySet().contains(key)){
                needParam.add(catToCatSetMap.get(key));
            }else {
                needParam.add(key);
            }

        }
        return needParam;

    }

    public void updateKVStore(String key, Map<String, Float> map) throws IOException, ClassNotFoundException {
        // 需要先判断是不是划分里的参数，如果是那么按照paramSet的方式更新，如果不是直接按照catParam的方式更新
        List<Set> ls_params = ls_partitionedVSet[ServerContext.serverId];
        long index = -1;

        for (Set<Long> paramKeySet : ls_params) {
            if (paramKeySet.contains(Long.parseLong(key.split("p")[1]))) {
                index = ls_params.indexOf(paramKeySet);
                break;
            }
        }


        if (index == -1) {
            Param param = (Param) TypeExchangeUtil.toObject(db.get(TypeExchangeUtil.toByteArray(key)));
            db.delete(TypeExchangeUtil.toByteArray(key));
            db.put(TypeExchangeUtil.toByteArray(key), TypeExchangeUtil.toByteArray(param.value + map.get(key)));
        } else {
            Set<Param> paramSet = (Set<Param>) TypeExchangeUtil.toObject(db.get(("catParamSet" + index).getBytes()));
            for (Param param : paramSet) {
                if (param.key.equals(key)) {
                    param.value += map.get(key);
                    db.delete(("s" + index).getBytes());
                    db.put(("s" + index).getBytes(), TypeExchangeUtil.toByteArray(paramSet));
                    break;
                }
            }


        }


    }


    public void initParamsLMF(Long userNum,Long movieNum,int r, Set<Long>[] vSet, List<Set> ls_params) throws IOException {

        // 这是按照最初的取余进行分配的
        // 对所有参数进行取余的分配，之后还会删掉存在别人server中的v
        for (long i = 0; i < userNum+movieNum; i++) {
            if (i % Context.serverNum == ServerContext.serverId) {
                RowOrColParam rowOrColParam=new RowOrColParam(r);
                for(int j=0;j<rowOrColParam.param.length;j++){
                    rowOrColParam.param[j]=RandomUtil.getRandomValue(-0.1f,0.1f);
                    rowOrColParam.index="p"+i;
                }
                db.put(("p" + i).getBytes(), TypeExchangeUtil.toByteArray(rowOrColParam));
//                System.out.println("params:" + i);
            }
        }

        // 还有一部分数据是划分到这台服务器上，但是既不在catParamSet，又不在取余的参数中，而在Vset中


        // 这里不能简单的key，value分配了，因为已经进行磁盘划分，那么就有集合的形式了
        for (Set set : ls_params) {
            Set<RowOrColParam> paramSet = new HashSet<RowOrColParam>();
            for (Object l : set) {
                RowOrColParam rowOrColParam=new RowOrColParam(r);
                for(int j=0;j<rowOrColParam.param.length;j++){
                    rowOrColParam.param[j]=RandomUtil.getRandomValue(-0.1f,0.1f);
                    rowOrColParam.index="p"+l;
                }

                paramSet.add(rowOrColParam);
            }
            db.put(("s" + ls_params.indexOf(set)).getBytes(), TypeExchangeUtil.toByteArray(paramSet));
        }


//        for(long i:vSet[ServerContext.serverId]){
//            db.put(("catParam"+i).getBytes(),TypeExchangeUtil.toByteArray(RandomUtil.getRandomValue(-0.1f,0.1f)));
//            System.out.println("params:"+i);
//        }

        // 再把放在别的server里vset删除
        for (int i = 0; i < vSet.length; i++) {
            if (i != ServerContext.serverId) {
                for (long l : vSet[i]) {
                    if (db.get(("p" + l).getBytes()) != null) {
                        db.delete(("p" + l).getBytes());
                    }
                }
            }
        }




        localParitionVSet = ls_partitionedVSet[ServerContext.serverId];
        for (Set<Long> set : localParitionVSet) {
            for (Long l : set) {
                catToCatSetMap.put("p" + l, "s" + localParitionVSet.indexOf(set));
            }
        }
    }


    @Synchronized
    public SRListMessage getNeededParams_LMF(Set<String> set) throws ClassNotFoundException, IOException {
        CurrentTimeUtil.setStartTime();
        DB db = ServerContext.kvStoreForLevelDB.getDb();

        // map仅存储需要的，而paramMap可能会包括一些catParamSet多余的参数
        Map<String, Float[]> map = new HashMap<String, Float[]>();
        List<Set> paramKeySetList = ls_partitionedVSet[ServerContext.serverId];
        long index = -1;
        // 存储需要访问的参数，也就是包含划分块的参数。catParamSet和catParam
        Set<String> needParam = new HashSet<String>();
        // 将需要用到的参数取出来，形成一个map
        Map<String, Float[]> paramMap = new HashMap<String, Float[]>();

        // 先转化需要取出来哪些参数
        needParam = getNeedPartitionParam(set);


        // 构建参数map


        CurrentTimeUtil.setStartTime();
        long startTime = 0;
        long endTime = 0;
        startTime=System.currentTimeMillis();
        for (String str : needParam) {
            if (str.indexOf("s") == -1) {
                if (db.get(str.getBytes()) == null) {
                    logger.info("nullstr:" + str);
                }
                RowOrColParam p = (RowOrColParam) TypeExchangeUtil.toObject(db.get(str.getBytes()));

                paramMap.put(p.index,p.param);
            } else {
                Set<RowOrColParam> temp_catParamSet =  (Set<RowOrColParam>) TypeExchangeUtil.toObject(db.get(str.getBytes()));
                for (RowOrColParam param : temp_catParamSet) {
                    paramMap.put(param.index, param.param);
                }
            }
        }
        endTime=System.currentTimeMillis();
        totleTimeOfgetParams += endTime - startTime;
        System.out.println(totleTimeOfgetParams);


        for (String key : set) {
            map.put(key, paramMap.get(key));
        }

        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("从map中获取维度的时间");
        return MessageDataTransUtil.Map_2_SRListMessage(map);
    }



    @Synchronized
    public void updateParamsLMF(Map<String, Float[]> map) {

        // 需要在内存中建一个map是从catParam到catSetParam的映射

        Set<String> needParam = getNeedPartitionParam(map.keySet());


        List<Set> localParitionVSet = ls_partitionedVSet[ServerContext.serverId];
        // 先将需要用到的参数读取到内存中,然后更新
        // 要更新的就三种参数，featParam在内存中好更新
        // 还剩catParam和catParamSet，那就建两个数据结构

        // 存储catParam
        Map<String, Float[]> catParamMap = new HashMap<String, Float[]>();
        Map<String, Set<RowOrColParam>> catParamSetMap = new HashMap<String, Set<RowOrColParam>>();
        Map<String, Float[]> allCatParamMap = new HashMap<String, Float[]>();
        Map<String, Set<RowOrColParam>> updateCatParamSetMap = new HashMap<String, Set<RowOrColParam>>();
        Map<String, Float[]> updateCatParamMap = new HashMap<String, Float[]>();


        // allCatParamMap是将set分开后，包含所有参数的（需要更新的）
        for (String index : needParam) {
//                System.out.println(index);
            try {
                 if (index.contains("s")) {
                    if (!catParamSetMap.keySet().contains(index)) {
                        Set<RowOrColParam> paramSetTemp = (Set<RowOrColParam>) TypeExchangeUtil.toObject(db.get(index.getBytes()));
                        catParamSetMap.put(index, paramSetTemp);
                        for (RowOrColParam param : paramSetTemp) {
                            allCatParamMap.put(param.index, param.param);
                        }
                    }
                } else {
                     RowOrColParam param = (RowOrColParam) TypeExchangeUtil.toObject(db.get(index.getBytes()));
                     catParamMap.put(index, param.param);
                     allCatParamMap.put(index, param.param);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


        for (String index : map.keySet()) {
            Float[] f=new Float[map.get(index).length];
                for(int i=0;i<map.get(index).length;i++){
                     f[i] = allCatParamMap.get(index)[i] - map.get(index)[i];
                }
                allCatParamMap.remove(index);
                allCatParamMap.put(index, f);
        }

        // 构造新的catParamSet以及catParam
        for (String index : allCatParamMap.keySet()) {
            //
            if (catToCatSetMap.containsKey(index)) {
                String index_catParamSet = catToCatSetMap.get(index);
                Set<RowOrColParam> paramSet = updateCatParamSetMap.get(index_catParamSet);
                RowOrColParam param = new RowOrColParam(index, allCatParamMap.get(index));
                if (paramSet != null) {
                    paramSet.add(param);
                } else {
                    paramSet = new HashSet<RowOrColParam>();
                    paramSet.add(param);
                    updateCatParamSetMap.put(index_catParamSet, paramSet);
                }
            } else {
                updateCatParamMap.put(index, allCatParamMap.get(index));
            }
        }




        // 是把更新后的数据放到updateCatParamMap

        try {
            for (String str : updateCatParamMap.keySet()) {
                db.delete(str.getBytes());
                RowOrColParam param = new RowOrColParam(str, updateCatParamMap.get(str));
                db.put(str.getBytes(), TypeExchangeUtil.toByteArray(param));
            }

            for (String str : updateCatParamSetMap.keySet()) {
                db.delete(str.getBytes());
                db.put(str.getBytes(), TypeExchangeUtil.toByteArray(updateCatParamSetMap.get(str)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



    }


}