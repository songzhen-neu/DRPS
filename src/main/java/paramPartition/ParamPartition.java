package paramPartition;

import Util.PartitionUtil;
import Util.RandomUtil;
import Util.SetUtil;
import Util.TypeExchangeUtil;
import context.Context;
import context.WorkerContext;

import dataStructure.sample.SampleList;
import org.iq80.leveldb.DB;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static Util.PartitionUtil.buildParamAccessNum;

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
    static Set<Long> catPrunedRecord = new HashSet<Long>();

    public static Set[] partitionV(){
        Set[] vSet = SetUtil.initSetArray(Context.serverNum);
        buildVAccessNum();
        return vSet;
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

                catPrunedRecord = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushVANumAndGetCatPrunedRecord(vAccessNum);



                set.clear();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }





}