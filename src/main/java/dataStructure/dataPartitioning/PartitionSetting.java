package dataStructure.dataPartitioning;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-16 13:43
 */
public class PartitionSetting {
    public int dataPartitionNum;
    public int partitionedDataSize;
    public String dataPath;

    public PartitionSetting(int dataPartitionNum,int partitionedDataSize,String dataPath){
        this.dataPartitionNum=dataPartitionNum;
        this.partitionedDataSize=partitionedDataSize;
        this.dataPath=dataPath;
    }
}