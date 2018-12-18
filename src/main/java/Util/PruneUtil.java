package Util;


import context.Context;
import context.WorkerContext;
import dataStructure.partition.Partition;
import dataStructure.partition.PartitionList;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import org.iq80.leveldb.DB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PruneUtil {
    public static List<Integer> prune(float pruneRate, double threshold, int samplePrunedSize)throws ClassNotFoundException,IOException{
        /**
        *@Description: 通过统计各个维度发生的频率，按照频率大小，得到剪枝之后的维度
        *@Param: [sampleList, pruneRate,
         * samplePrunedSize：剪枝所用到的数据集大小]
        *@return: java.util.List<java.lang.Integer>
        *@Author: SongZhen
        *@date: 下午3:11 18-11-19
        */
        // 先遍历数据将数据每个维度出现的次数做成一个数组
        // sparseDimSize是所有数据集可能出现的维度
        DB db=WorkerContext.kvStoreForLevelDB.getDb();
        long sparseDimSize=Context.sparseDimSize;
        Double freqThreshold;
        List<Integer> prunedSparseDim=new ArrayList<Integer>();


        computeCountSparseDimFreq();

//        UpdateDoublesSketch updateDoublesSketch=DoublesSketch.builder().build();
//        for(int i=0;i<sparseDimSize;i++){
//            updateDoublesSketch.update(countSparseDimFreq[i]);
//        }
//
//
//        freqThreshold=updateDoublesSketch.getQuantile(1.0-pruneRate);
//
//        // 这里表示不使用剪枝率，让大家的threshold都一样
//        if(!Context.usePruneRate){
//            freqThreshold=threshold;
//        }
//
//
//        System.out.println("threshold:"+freqThreshold);
//        for(int i=0;i<countSparseDimFreq.length;i++){
//            if(countSparseDimFreq[i]>=freqThreshold){
//                prunedSparseDim.add(i);
//            }
//        }
//
//        return prunedSparseDim;
        return  null;
    }

    static void computeCountSparseDimFreq() throws IOException,ClassNotFoundException {
        DB db=WorkerContext.kvStoreForLevelDB.getDb();
        for(int i=0;i<WorkerContext.sampleBatchListPrunedSize;i++){
            byte[] bytes=db.get(("batchSample"+i*(WorkerContext.sampleBatchListSize/WorkerContext.sampleBatchListPrunedSize)+WorkerContext.workerId).getBytes());
            SampleList batch=(SampleList) TypeExchangeUtil.toObject(bytes);
            for(Sample sample:batch.sampleList){
                for(long cat:sample.cat){
                    byte[] numOfCat=db.get(("numOfCat"+cat).getBytes());
                    if(numOfCat!=null){
                        long num=(Long)TypeExchangeUtil.toObject(numOfCat);
                        num++;
                        db.delete(("numOfCat"+cat).getBytes());
                        db.put(("numOfCat"+cat).getBytes(),TypeExchangeUtil.toByteArray(num));
                    }else {
                        db.put(("numOfCat"+cat).getBytes(),TypeExchangeUtil.toByteArray(1));
                    }
                }
            }
        }
    }

    public static List<Integer> removeOnceItemOfBPL(PartitionList bestPartitionList){
        List<Integer> prunedDimWithNoOnceItem=new ArrayList<Integer>();
        for(Partition partition:bestPartitionList.partitionList){
            if(partition.partition.size()>(WorkerContext.minPartitionSize-1)){
                for(int cat:partition.partition){
                    prunedDimWithNoOnceItem.add(cat);
                }
            }
        }

        return prunedDimWithNoOnceItem;
    }

    public static int getIndexOfPrunedDim(int dim,List<Integer> prunedSparseDim){
        return prunedSparseDim.indexOf(dim);
    }
}
