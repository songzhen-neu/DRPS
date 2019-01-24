package Util;

import context.Context;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import net.PSWorker;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @program: simplePsForModelPartition
 * @description: 进行划分的工具包
 * @author: SongZhen
 * @create: 2019-01-18 10:32
 */
public class PartitionUtil {
    static Logger logger=LoggerFactory.getLogger(PartitionUtil.class.getName());
    static DB db=WorkerContext.kvStoreForLevelDB.getDb();
    static Set<Integer> batchSampledRecord=new HashSet<Integer>();
    static Set<Long> catPrunedRecord=new HashSet<Long>();
    // 这个是放在内存里的vAccessNum
    static Map<Long,Integer> vAccessNum=new HashMap<Long, Integer>();


    public static Set[] partitionV() throws IOException,ClassNotFoundException{
        boolean isInited=false;
        float Ti_com=0;
        float Ti_disk=0;
        int insertI=0;
        // 本地存储哪台机器（server）存了哪些参数
        Set[] vSet = SetUtil.initSetArray(Context.serverNum);

        // 统计每个参数被本地的batch访问的次数，并放到worker的数据库里，以vAccessNum开头,
        buildVAccessNum();


        // 上述是已经采样完的数据的关于V的访问的统计，其中batchRecord是采样的batch的index
        // 下面开始进行维度的剪枝，返回的是server计算完成之后，被剪枝后的维度
        // 所以每台机器都要向master发送采样后，每个V被访问的次数
        catPrunedRecord=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushVANumAndGetCatPrunedRecord(vAccessNum);
        logger.info("prunedVSet:"+catPrunedRecord.size());


        // 下面取出j=1，放在第insertI台机器上
        long j_last=0;
        List<Set> partitionedVSet;
        for (long j : catPrunedRecord) {
            if (!isInited) {
                // 也就是初始化Ticom和Ti_disk
                Ti_com = getInitTiComInMemory(catPrunedRecord);


                Ti_disk = 0;


                isInited = true;
            } else {
                // 统计本机访问Vi的次数
                float T_localAccessVj = getVjAccessNumInMemory(j_last);
                // 统计其他机器访问Vi的次数
                if (insertI == WorkerContext.workerId) {
                    // 这些都还是对j_last插入后，做的Tdisk和Tcom的更新计算
//                    System.out.println("hhaha1");
                    float accessNum_otherWorkers = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pullOtherWorkerAccessForVi();
//                    System.out.println("haha2");
                    Ti_com = Ti_com - T_localAccessVj + accessNum_otherWorkers;

                    // 下面开始计算disk的时间,也是只修改插入的Tdisk的值。
                    // 先从server中获取vSet[insertId]的参数分配
//                    partitionedVSet=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pullPartitionedVset();




                } else {
//                    System.out.println("haha3");
                    WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushLocalViAccessNum(T_localAccessVj);
//                    System.out.println("haha4");
                }
            }

//            System.out.println("haha5");
//            WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();

            PSWorker psWorker = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId);
            insertI = psWorker.sentInitedT(Ti_com * Context.netTrafficTime + Ti_disk);


//            System.out.println("haha6");

            logger.info("insert "+j+" into "+insertI);
            // 发送给server master，然后选出一个耗时最短的机器i，然后作为加入j的机器

            vSet[insertI].add(j);
            j_last=j;

//            System.out.println("setSize:"+(vSet[0].size()+vSet[1].size()+vSet[2].size()));
        }

        return vSet;

    }

    private static float getVjAccessNum(long j) throws IOException,ClassNotFoundException{
        int num=0;
        if(db.get(("vAccessNum"+j).getBytes())!=null){
            num=(Integer) TypeExchangeUtil.toObject(db.get(("vAccessNum"+j).getBytes()));
        }else {
            num=0;
        }
        return num;
    }

    private static float getVjAccessNumInMemory(long j) throws IOException,ClassNotFoundException{
        int num=0;
        if(vAccessNum.get(j)!=null){
            num=vAccessNum.get(j);
        }else {
            num=0;
        }
        return num;
    }


    private static float getInitTiComInMemory(Set<Long> catPrunedRecord){
        /**
        *@Description: 初始的Ticom，就是所有的本地的需要访问的参数的次数。
         * 也就是看每个参数被batch访问次数之和
        *@Param: [catPrunedRecord]
        *@return: float
        *@Author: SongZhen
        *@date: 上午8:22 19-1-22
        */
        float sum=0;
        for(long l:catPrunedRecord){
            if(vAccessNum.get(l)!=null){
                sum+=vAccessNum.get(l);
            }
        }
        return sum;
    }

    private static float getInitTiCom()throws IOException,ClassNotFoundException{
        float sum=0;
        for(int i=0;i<Context.sparseDimSize;i++){
            if(db.get(("vAccessNum"+i).getBytes())!=null){
                sum+=(Integer) TypeExchangeUtil.toObject(db.get(("vAccessNum"+i).getBytes()));
            }
        }

        return sum;
    }

    private static void buildVAccessNum(){
        DB db=WorkerContext.kvStoreForLevelDB.getDb();
        Set<Long> set=new HashSet<Long>();
        int num_ContainsBatchPruned=WorkerContext.sampleBatchListSize/WorkerContext.sampleBatchListPrunedSize;


        // 遍历数据并统计Ui访问的参数v的个数
        for(int i=0;i<WorkerContext.sampleBatchListPrunedSize;i++){
            try{
                int m=RandomUtil.getIntRandomFromZeroToN(num_ContainsBatchPruned);
                int index=i*(num_ContainsBatchPruned)+m;
                SampleList sampleBatch=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+index).getBytes()));
                batchSampledRecord.add(i*(num_ContainsBatchPruned)+m);

                // 这里的set是采样后的每个sampleBatch
                buildVSetOfBatch(set, sampleBatch);
                // 遍历set，然后更新db里的对V的访问次数，db里没有出现的维度，说明本地数据集对这个维度没有访问
                // 这个是基于磁盘的
//                buildVSetAccessNumOfBatch(set);
                // 由于经过了采样和剪枝，那么其实可以基于内存做
                buildVSetAccessNumOfBatchInMemory(set);
                set.clear();
            }catch (IOException e){
                e.printStackTrace();
            }catch (ClassNotFoundException e){
                e.printStackTrace();
            }
        }

    }

    private static void buildVSetOfBatch(Set<Long> set, SampleList sampleBatch) {
        /**
        *@Description: 这个batch访问的所有稀疏维度cat的集合
        *@Param: [set, sampleBatch]
        *@return: void
        *@Author: SongZhen
        *@date: 上午9:29 19-1-21
        */
        for(int j=0;j<sampleBatch.sampleList.size();j++){
            for(Sample sample:sampleBatch.sampleList){
                long[] catList=sample.cat;
                for(long cat:catList){
                    if(cat!=-1){
                        set.add(cat);
                    }
                }
            }
        }
    }

    private static void buildVSetAccessNumOfBatch(Set<Long> set){
        try{
            for(long i:set){
                if(db.get(("vAccessNum"+i).getBytes())!=null){
                    Integer viAccessNum=(Integer)TypeExchangeUtil.toObject(db.get(("vAccessNum"+i).getBytes()));
                    viAccessNum++;
                    db.put(("vAccessNum"+i).getBytes(),TypeExchangeUtil.toByteArray(viAccessNum));
//                    System.out.println("i:"+i+"viAccessNum:"+viAccessNum);
                }else {
                    db.put(("vAccessNum"+i).getBytes(),TypeExchangeUtil.toByteArray(1));
                }


            }
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }


    private static void buildVSetAccessNumOfBatchInMemory(Set<Long> set){
        for(long i:set){
            if(vAccessNum.get(i)!=null){
                int num=vAccessNum.get(i);
                num++;
                vAccessNum.put(i,num);
            }else {
                vAccessNum.put(i,1);
            }


        }
    }


}