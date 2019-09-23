package context;

import dataStructure.enumType.ParallelismControlModel;

/**
 * @program: simplePsForModelPartition
 * @description: 快速设置参数
 * @author: SongZhen
 * @create: 2019-09-23 15:44
 */
public class QuickSetting {
    public static String algorithmName="SVM";
    public static Boolean isOptimizeDisk=true;
    public static Boolean isOptimizeNetTraffic=false;
    public static Boolean isUseOptimalIndex=true;
    public static int freqThreshold=10;
    public static int workerNum=1;
    public static int serverNum=1;
    public static ParallelismControlModel parallel=ParallelismControlModel.BSP;
    public static int workerId=0;
    public static int serverId=0;


}