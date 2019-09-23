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
    public long sparseDimSize;
    public String algorithmName;

    public GeneralAlgorithmSetting(int sampleListSize,int samplePrunedSize,int sampleBatchSize,String myDataPath,int catSize,int featureSize,long sparseDimSize, String algorithmName){
        this.sampleListSize=sampleListSize;
        this.samplePrunedSize=samplePrunedSize;
        this.sampleBatchSize=sampleBatchSize;
        this.myDataPath=myDataPath;
        this.catSize=catSize;
        this.featureSize=featureSize;
        this.sampleBatchListSize=sampleListSize/sampleBatchSize;
        this.sampleBatchListPrunedSize=samplePrunedSize/sampleBatchSize;
        this.sparseDimSize=sparseDimSize;
        this.algorithmName=algorithmName;

    }

}