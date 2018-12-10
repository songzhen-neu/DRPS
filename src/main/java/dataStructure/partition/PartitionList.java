package dataStructure.partition;
import context.Context;

import java.util.ArrayList;
import java.util.List;

public class PartitionList{
    public List<Partition> partitionList=new ArrayList<Partition>();

    public static PartitionList getBestPartitionListWithNoOnceItem(PartitionList bestPartitionList){
        /**
         *@Description: 如果划分中只有一个元素，那么没有必要按照KVParaPartition读
         *@Param: [bestPartitionList]
         *@return: ParaStructure.Partitioning.PartitionList
         *@Author: SongZhen
         *@date: 上午9:53 18-11-13
         */
        PartitionList bestPartitionListWithNoOnceItem=new PartitionList();
        for(int i=0;i<bestPartitionList.partitionList.size();i++){
            Partition partition=bestPartitionList.partitionList.get(i);
            if(partition.partition.size()>(Context.minPartitionSize-1)){
                bestPartitionListWithNoOnceItem.partitionList.add(partition);
            }
        }

        return bestPartitionListWithNoOnceItem;
    }

    public  void showPartitionList(){
        for(Partition partition: this.partitionList){
            System.out.println(partition.partition);
        }
    }

    public static PartitionList seqIndexToRealIndex(PartitionList partitionList,List<Integer> prunedSparseDim){
        /**
         *@Description: 将1,2,3，4的顺序序列转化成100，546,714,1115的真实索引值
         *@Param: [partitionList, prunedSparseDim]
         *@return: ParaStructure.Partitioning.PartitionList
         *@Author: SongZhen
         *@date: 下午8:40 18-11-21
         */
        PartitionList bestPartitionList=new PartitionList();
        for(int i=0;i<partitionList.partitionList.size();i++){
            List<Integer> partition=partitionList.partitionList.get(i).partition;
            Partition newPartition=new Partition();
            for(int j=0;j<partition.size();j++){
                newPartition.partition.add(prunedSparseDim.get(partition.get((j))));
            }
            bestPartitionList.partitionList.add(newPartition);
        }

        return  bestPartitionList;
    }


}
