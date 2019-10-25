package context;

import dataStructure.enumType.ParallelismControlModel;

/**
 * @program: simplePsForModelPartition
 * @description: 快速设置参数
 * @author: SongZhen
 * @create: 2019-09-23 15:44
 */
public class QuickSetting {
    public static String algorithmName="LoR";
    public static Boolean isOptimizeDisk=false;
    public static Boolean isOptimizeNetTraffic=false;
    public static Boolean isUseOptimalIndex=true;
    public static int freqThreshold=25;
    public static int workerNum=3;
    public static int serverNum=3;
    public static ParallelismControlModel parallel=ParallelismControlModel.SSP;
    public static int workerId=0;
    public static int serverId=0;
    public static float minGain=20f;
    public static int K_mergeIndex=1000;
    public static int cluster=1;


}