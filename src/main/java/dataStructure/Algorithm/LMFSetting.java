package dataStructure.Algorithm;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-16 14:26
 */
public class LMFSetting {
    public int sampleListSize;
    public int samplePrunedSize;
    public int sampleBatchSize;
    public int sampleBatchListSize;
    public int sampleBatchListPrunedSize;
    public String dataPath;
    public int userNum;
    public int movieNum;
    public int r;

    public LMFSetting(int sampleListSize,int samplePrunedSize,int sampleBatchSize,int r,int userNum,int movieNum,String dataPath){
        this.sampleListSize=sampleListSize;
        this.samplePrunedSize=samplePrunedSize;
        this.sampleBatchSize=sampleBatchSize;
        this.sampleBatchListSize=sampleListSize/sampleBatchSize;
        this.sampleBatchListPrunedSize=samplePrunedSize/sampleBatchSize;
        this.r=r;
        this.userNum=userNum;
        this.movieNum=movieNum;
        this.dataPath=dataPath;

    }
}