package dataStructure.Algorithm;

import dataStructure.enumType.ParallelismControlModel;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-16 15:12
 */
public class GeneralContextSetting {
    public int featureSize;
    public int workerNum;
    public int serverNum;
    public int freqThreshold;
    public ParallelismControlModel parallelismControlModel;
    public String algorithmName;


    public GeneralContextSetting(int featureSize, int workerNum,int serverNum,int freqThreshold,ParallelismControlModel parallelismControlModel,String algorithmName){
        this.featureSize=featureSize;
        this.workerNum=workerNum;
        this.serverNum=serverNum;
        this.freqThreshold=freqThreshold;
        this.parallelismControlModel=parallelismControlModel;
        this.algorithmName=algorithmName;
    }

}