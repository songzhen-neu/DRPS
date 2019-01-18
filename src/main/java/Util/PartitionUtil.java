package Util;

import context.Context;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import net.PSWorker;
import org.iq80.leveldb.DB;
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
    public static void partitionV(){
        // 也就是初始化Ticom和Ti_disk
        float Ti_com=getAccessedVNum();
        float Ti_disk=0;

        // 发送给server master，然后选出一个耗时最短的机器i，然后作为加入j的机器
        PSWorker psWorker=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId);
        psWorker.sentInitedT(Ti_com+Ti_disk);
    }

    public static long getAccessedVNum()  {
        long vNumAccessedByU=0;
        DB db =WorkerContext.kvStoreForLevelDB.getDb();
        Set<Long> set=new HashSet<Long>();

        // 遍历数据并统计Ui访问的参数v的个数
        for(int i=0;i<WorkerContext.sampleBatchListSize;i++){
            try{
                SampleList sampleBatch=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+i).getBytes()));
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
            }catch (IOException e){
                e.printStackTrace();
            }catch (ClassNotFoundException e){
                e.printStackTrace();
            }
        }
        vNumAccessedByU=set.size();
        logger.info("localUAccessVSize:"+set.size());

        return vNumAccessedByU;
    }
}