package paramPartition;

import Util.PartitionUtil;
import Util.RandomUtil;
import Util.SetUtil;
import Util.TypeExchangeUtil;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import context.Context;
import context.WorkerContext;

import dataStructure.partition.AFMatrix;
import dataStructure.partition.Partition;
import dataStructure.partition.PartitionList;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import io.netty.util.internal.ConcurrentSet;
import net.BMessage;
import org.iq80.leveldb.DB;
import store.KVStoreForLevelDB;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

import static Util.PartitionUtil.buildParamAccessNum;
import static context.WorkerContext.samplePrunedSize;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-06 09:38
 */
public class ParamPartition {
    // 这个是放在内存里的vAccessNum
    static Map<Long, Integer> vAccessNum = new HashMap<Long, Integer>();
    static Set<Integer> batchSampledRecord = new HashSet<Integer>();
    static List<Long> catPrunedRecord = new ArrayList<Long>();
    static PartitionList bestPartitionList;

    // server是否已经达到精度，完成partition任务
    static boolean isFinishedPartition=false;

    public static Set[] partitionV() throws UnknownHostException,ClassNotFoundException,IOException {
        Set[] vSet = SetUtil.initSetArray(Context.serverNum);
        buildVAccessNum();
        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();
        catPrunedRecord = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushVANumAndGetCatPrunedRecord_ParamPartition(vAccessNum);

        // 向server获取当前划分，并且不断更新
        while (!isFinishedPartition){
            // 获取当前的partition
            bestPartitionList=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getBestPartition();

            // 根据当前的partitionList，每个worker都去统计AFMatrix，并将AF矩阵push到master中
            int[][] afMatrix=buildAFMatrix(bestPartitionList,batchSampledRecord,catPrunedRecord);

            // 将afMatrix发送给server(master)，并在server(master)中合并
            isFinishedPartition=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).sendAFMatrix(afMatrix);


        }

        System.out.println("111");

        return vSet;
    }


    private static int[][] buildAFMatrix(PartitionList partitionList,Set<Integer> batchSampledRecord,List<Long> catPrunedRecord) throws IOException,ClassNotFoundException {
        /**
         *@Description: 建立AF矩阵类，包含af矩阵、当前划分、组合时间成本、时间成本减少值
         *@Param: [partitionList, sampleList, prunedSparseDim, samplePrunedSize]
         *@return: ParaStructure.Partitioning.AFMatrix
         *@Author: SongZhen
         *@date: 上午9:08 18-11-28
         */
        int catSize ;
        int partitionListSize = partitionList.partitionList.size();
        int[][] afMatrix = new int[partitionListSize][partitionListSize];
        DB db=WorkerContext.kvStoreForLevelDB.getDb();
        Set<Long> setPrunedSparseDim=TypeExchangeUtil.List_2_LongSet(catPrunedRecord);

        for (int i :batchSampledRecord) {  //这是个大循环，在循环所有的数据集
            SampleList sampleList=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+i).getBytes()));
            // 统计batch包含的cat
            Set<Long> batchVSet = buildBatchVSetBasedOnPruned(sampleList,setPrunedSparseDim);


            // 如果这一条数据的cat属性能够组合出来Partition，就说明这个partition在这条数据中出现了
            // 这里catContainsPartition存储的是第几个partition出现在batch的cat里了，所以用int（已经将维度转化成0,1,2,3,...)
            Set<Integer> catContainsPartition=new HashSet<Integer>();
            for(int l=0;l<partitionListSize;l++){
                Partition partition=partitionList.partitionList.get(l);
                int flag=0;
                for(int m=0;m<partition.partition.size();m++){
                    if(batchVSet.contains(partition.partition.get(m))){
                        // 这里无论怎么样，都是只包含一个就可以
                        catContainsPartition.add(l);
                        break;

                    }
                }

            }

            // 这个就是按照partitionList的顺序转化成0,1,2,...的
            for(int l:catContainsPartition){
                for(int m:catContainsPartition){
                    afMatrix[l][m]++;
                }
            }

        }

        return afMatrix;
    }

    public static Set<Long> buildBatchVSetBasedOnPruned(SampleList sampleBatch,Set<Long> setPrunedSparseDim) {
        Set<Long> list=new HashSet<Long>();
        for (Sample sample : sampleBatch.sampleList) {
            long[] catList = sample.cat;
            for (long cat : catList) {
                if (cat != -1&& setPrunedSparseDim.contains(cat)) {
                    list.add(cat);
                }
            }
        }
        return list;
    }


    public static void buildVAccessNum(){
        DB db = WorkerContext.kvStoreForLevelDB.getDb();
        Set<Long> set = new HashSet<Long>();
        int num_ContainsBatchPruned = WorkerContext.sampleBatchListSize / WorkerContext.sampleBatchListPrunedSize;


        // 遍历数据并统计Ui访问的参数v的个数
        for (int i = 0; i < WorkerContext.sampleBatchListPrunedSize; i++) {
            try {
                int m = RandomUtil.getIntRandomFromZeroToN(num_ContainsBatchPruned);
                int index = i * (num_ContainsBatchPruned) + m;
                SampleList sampleBatch = (SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch" + index).getBytes()));
                batchSampledRecord.add(i * (num_ContainsBatchPruned) + m);

                // 这里的set是采样后的每个sampleBatch
                PartitionUtil.buildBatchVSet(set, sampleBatch);

                // 遍历set，然后更新db里的对V的访问次数，db里没有出现的维度，说明本地数据集对这个维度没有访问
                // 由于经过了采样和剪枝，那么其实可以基于内存做
                PartitionUtil.buildParamAccessNum(set, vAccessNum);
                set.clear();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }





}