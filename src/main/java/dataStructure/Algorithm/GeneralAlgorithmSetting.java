package dataStructure.Algorithm;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-16 14:12
 */
public class GeneralAlgorithmSetting {
    public int sampleListSize;
    public int samplePrunedSize;
    public int sampleBatchSize;
    public String myDataPath;
    public int catSize;
    public int featureSize;
    public int sampleBatchListSize;
    public int sampleBatchListPrunedSize;

    public GeneralAlgorithmSetting(int sampleListSize,int samplePrunedSize,int sampleBatchSize,String myDataPath,int catSize,int featureSize){
        this.sampleListSize=sampleListSize;
        this.samplePrunedSize=samplePrunedSize;
        this.sampleBatchSize=sampleBatchSize;
        this.myDataPath=myDataPath;
        this.catSize=catSize;
        this.featureSize=featureSize;
        this.sampleBatchListSize=sampleListSize/sampleBatchSize;
        this.sampleBatchListPrunedSize=samplePrunedSize/sampleBatchSize;

    }

}