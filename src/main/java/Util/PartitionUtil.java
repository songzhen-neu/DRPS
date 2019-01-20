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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description: 进行划分的工具包
 * @author: SongZhen
 * @create: 2019-01-18 10:32
 */
public class PartitionUtil {
    static Logger logger=LoggerFactory.getLogger(PartitionUtil.class.getName());
    static DB db=WorkerContext.kvStoreForLevelDB.getDb();
    public static void partitionV() throws IOException,ClassNotFoundException{
        boolean isInited=false;
        float Ti_com=0;
        float Ti_disk=0;
        int insertI=0;
        // 本地存储哪台机器（server）存了哪些参数
        Set[] vSet = SetUtil.initSetArray(Context.serverNum);

        // 统计每个参数被本地的batch访问的次数，并放到worker的数据库里，以vAccessNum开头
        buildVAccessNum();




        // 下面取出j=1，放在第insertI台机器上
        for(long j=0;j<Context.sparseDimSize;j++){

            if (!isInited) {
                // 也就是初始化Ticom和Ti_disk
                Ti_com = getInitTiCom();


                Ti_disk = 0;

                // 发送给server master，然后选出一个耗时最短的机器i，然后作为加入j的机器
                PSWorker psWorker = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId);
                insertI = psWorker.sentInitedT(Ti_com * Context.netTrafficTime + Ti_disk);
                vSet[insertI].add(j);


                isInited = true;
            }else {
                // 统计本机访问Vi的次数
                float T_localAccessVj=getVjAccessNum(j);
                // 统计其他机器访问Vi的次数
                if(insertI==WorkerContext.workerId){
                    float accessNum_otherWorkers=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pullOtherWorkerAccessForVi();
                    Ti_com=Ti_com-T_localAccessVj+accessNum_otherWorkers;

                }else {
                    WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushLocalViAccessNum(T_localAccessVj);
                }
            }
        }

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

        // 遍历数据并统计Ui访问的参数v的个数
        for(int i=0;i<WorkerContext.sampleBatchListSize;i++){
            try{
                SampleList sampleBatch=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+i).getBytes()));
                buildVSetOfBatch(set, sampleBatch);
                // 遍历set，然后更新db里的对V的访问次数，db里没有出现的维度，说明本地数据集对这个维度没有访问
                buildVSetAccessNumOfBatch(set);
            }catch (IOException e){
                e.printStackTrace();
            }catch (ClassNotFoundException e){
                e.printStackTrace();
            }
        }

    }

    private static void buildVSetOfBatch(Set<Long> set, SampleList sampleBatch) {
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


}