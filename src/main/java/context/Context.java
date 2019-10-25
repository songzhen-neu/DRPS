package context;

import dataStructure.Algorithm.GeneralContextSetting;
import net.PSRouterClient;
import parallelism.SSP;
import parallelism.WSP;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import dataStructure.enumType.ParallelismControlModel;

/**
 * @program: simplePsForModelPartition
 * @description: 共同的context
 * @author: SongZhen
 * @create: 2018-12-17 20:55
 */


public class Context {


    /**
     * 网络通信server的相关配置
     */
    public static Map<Integer, String> serverIp = new HashMap<Integer, String>();
    public static Map<Integer, Integer> serverPort = new HashMap<Integer, Integer>();


    public static int featureSize;

    /**
     * 是否是分布式执行
     */
    public static boolean isDist;

    /**
     * 是否初始化完毕
     */
    public static boolean inited = false;

    /**
     * worker和server数量
     */
    public static int workerNum;
    public static int serverNum;

    /**
     * 数据集划分
     */
    public static int partitionedDataSize;
    public static int dataPartitionNum;

    /**
     * 判断是不是server的master机器，管参数分配的
     */
    public static int masterId;
    public static String dataPath;

    public static int maxMessageSize;
    public static float diskSeekTime;
    public static float diskAccessTime;
    public static float netTrafficTime;

    public static float freqThreshold;
    public static boolean usePruneRate;

    public static int setParamBaseSize_bytes;
    public static int singleParamOfSetSize_bytes;
    public static int floatSize_bytes;

    public static int freqThresholdForSingleMachine;
    public static int boundForSSP;

    public static ParallelismControlModel parallelismControlModel;
    public static PSRouterClient psRouterClient;

    public static CyclicBarrier[] cyclicBarrier_sub1;
    public static AtomicInteger trainRoundNum = new AtomicInteger(0);

    public static final float alpha_WSP = 0.01f;
    public static float minGain;

    /**
     * 最大磁盘划分的大小
     */
    public static int maxDiskPartitionNum;

    /**
     * 做对比实验，是否进行磁盘优化
     */
    public static boolean isOptimizeDisk;

    /**
     * 做对比实验，是否进行网络
     */
    public static boolean isOptimizeNetTraffic;
    // param用p表示，paramSet用s表示，feature用f表示

    /**
     * wsp中标准化时间，乘以一个倍数
     */
    public static float TimeMulWSP = 10f;

    public static int K_mergeIndex;

    public static boolean isUseOptimalIndex;


    public static GeneralContextSetting SVM = null;
    // dataset 179  train 10
    public static GeneralContextSetting LoR = null;
    public static GeneralContextSetting LiR = null;
    public static GeneralContextSetting LMF = null;

    /**
     * 稀疏维度大小
     */
    // 需要手动输入稀疏维度的个数
    public static long sparseDimSize;

    public static void init() throws IOException {
        if (inited == true) {
            return;
        }
        GeneralContextSetting SVM = new GeneralContextSetting(10, QuickSetting.workerNum, QuickSetting.serverNum, QuickSetting.freqThreshold, QuickSetting.parallel, "SVM");
        // dataset 179  train 10
        GeneralContextSetting LoR = new GeneralContextSetting(10, QuickSetting.workerNum, QuickSetting.serverNum, QuickSetting.freqThreshold, QuickSetting.parallel, "LoR");
        GeneralContextSetting LiR = new GeneralContextSetting(0, QuickSetting.workerNum, QuickSetting.serverNum, QuickSetting.freqThreshold, QuickSetting.parallel, "LiR");
        GeneralContextSetting LMF = new GeneralContextSetting(1, QuickSetting.workerNum, QuickSetting.serverNum, QuickSetting.freqThreshold, QuickSetting.parallel, "LMF");

        GeneralContextSetting generalContextSetting = null;
        switch (QuickSetting.algorithmName) {
            case "LiR":
                generalContextSetting = LiR;
                break;
            case "LoR":
                generalContextSetting = LoR;
                break;
            case "SVM":
                generalContextSetting = SVM;
                break;
            case "LMF":
                generalContextSetting = LMF;
                break;
            default:
                System.out.println("无该算法");
        }
        isOptimizeDisk = QuickSetting.isOptimizeDisk;
        isUseOptimalIndex = QuickSetting.isUseOptimalIndex;
        isOptimizeNetTraffic = QuickSetting.isOptimizeNetTraffic;
        minGain=QuickSetting.minGain;

        if(QuickSetting.cluster==1){
            serverIp.put(0, "202.199.13.120");
            serverIp.put(1, "202.199.13.120");
            serverIp.put(2, "202.199.13.120");
            serverPort.put(0, 9010);
            serverPort.put(1, 9011);
            serverPort.put(2, 9012);
        }else if(QuickSetting.cluster==2){
            serverIp.put(0,"219.216.64.103");
            serverPort.put(0,9010);
            for(int i=1;i<13;i++){
                if(i<9){
                    serverIp.put(i, "192.168.111.10"+(i+1));
                    serverPort.put(i, 9010);
                }else {
                    serverIp.put(i, "192.168.111.1"+(i+1));
                    serverPort.put(i, 9010);
                }

            }


        }

        boundForSSP = 2;
        K_mergeIndex=QuickSetting.K_mergeIndex;
        setParamBaseSize_bytes = 128;
        singleParamOfSetSize_bytes = 22;

//        featureSize=10;
//        featureSize=2;
        featureSize = generalContextSetting.featureSize;

        isDist = true;


        // 当worker和server数量都是1，则为单机运行，masterId设置为0
        workerNum = generalContextSetting.workerNum;
        serverNum = generalContextSetting.serverNum;
        dataPartitionNum = workerNum;
        masterId = 0;


        maxMessageSize = Integer.MAX_VALUE;

        inited = true;

        diskSeekTime = 1f;
        diskAccessTime = 0.001f;
//        netTrafficTime=0.0000095f;
        netTrafficTime = 0.00000095f;
        floatSize_bytes = 79;

//        freqThresholdForSingleMachine=0;
//        freqThreshold=freqThresholdForSingleMachine*workerNum;   // 表示大于freqThreshold这个频率的
        freqThreshold = generalContextSetting.freqThreshold;
        usePruneRate = true;

        psRouterClient = new PSRouterClient();
        parallelismControlModel = generalContextSetting.parallelismControlModel;
        switch (parallelismControlModel) {
            case SSP:
                SSP.init();
                break;
            case SSP_S:
                SSP.init();
                break;
            case WSP:
                WSP.init();
                break;
        }

        if (workerNum > 1) {
            cyclicBarrier_sub1 = new CyclicBarrier[workerNum];
            for (int i = 0; i < cyclicBarrier_sub1.length; i++) {
                cyclicBarrier_sub1[i] = new CyclicBarrier(workerNum - 1);
            }
        }

    }
}
