package dataStructure.partition;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import context.Context;
import context.WorkerContext;

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
            if(partition.partition.size()>(WorkerContext.minPartitionSize-1)){
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




}
