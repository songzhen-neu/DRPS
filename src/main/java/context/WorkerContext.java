package context;

import dataStructure.Algorithm.GeneralAlgorithmSetting;
import dataStructure.Algorithm.LMFSetting;
import net.PSRouterClient;
import net.PSWorker;
import store.KVStoreForLevelDB;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-10 18:46
 */
public class WorkerContext {
//    public static PSWorker psWorker;
    public static PSRouterClient psRouterClient;

    public static int workerId;
    /** 判断是分布式执行还是单机执行*/
    public static enum Mode{
        STANDALONE, DISTRIBUTED
    }

    public static Mode mode;

    /** 路径*/
    public static String myDataPath; // 本地训练用到的数据集（划分后）
    public static String myDataPath_LMF;

    /** cat是否在feature前面*/
    public static boolean isCatForwardFeature;

    /** 用于训练的样本个数*/
    public static int sampleListSize;

    /** batch相关信息*/
    public static int sampleBatchListSize; // 训练集包含batch的数目
    public static int sampleBatchSize;  // 一个batch的大小
    public static int inMemSampleBatchNum;  // 内存中可以存下batch的最大数量


    public static int sampleBatchListSize_LMF; // 训练集包含batch的数目
    public static int sampleBatchSize_LMF;  // 一个batch的大小
    public static int sampleListSize_LMF; // 训练集的样本个数
    public static int sampleBatchListPrunedSize_LMF;
    public static  int samplePrunedSize_LMF;


    /** 剪枝*/

    public static float pruneRate;
    public static int samplePrunedSize;   // 用于剪枝和模型划分的训练样本个数
    public static int sampleBatchListPrunedSize; // 用于剪枝和模型划分的训练batch个数

    /** 原始数据集相关信息*/
    public static int catSize;

    /** 模型划分相关信息*/
    public static int minPartitionSize;

    /** 磁盘上的k-v数据库*/
    public static KVStoreForLevelDB kvStoreForLevelDB=new KVStoreForLevelDB();
    public static String levelDBPathForWorker;

    /** 用户数*/
    public static long userNum_LMF;
    /** 电影数*/
    public static long movieNum_LMF;
    /** 矩阵分解的隐含量*/
    public static int r_LMF;

    public static GeneralAlgorithmSetting LiR;
    public static GeneralAlgorithmSetting LoR;
    public static GeneralAlgorithmSetting SVM;
    public static LMFSetting LMF;
    public static GeneralAlgorithmSetting generalSetting;







    public static void init()throws IOException {

        workerId=QuickSetting.workerId;
        mode=Mode.DISTRIBUTED;
        isCatForwardFeature=true;

        // housing 9 0  data 13 7
        LiR=new GeneralAlgorithmSetting(2000,200,200,
                "data/LiRData/skewed"+workerId+".csv",9,0,1000000000,"LiR");
        // train 12 10
        // dataset 7 179
        LoR=new GeneralAlgorithmSetting(100000,100000,10000,
                "data/LoRData/train"+workerId+".csv",12,10,1000000,"LoR");
        SVM=new GeneralAlgorithmSetting(30000,30000,30000,
                "data/SVMData/train"+workerId+".csv",12,10,23742,"SVM");

        // 用户1～53424，电影1~10000，样本数量5976479
        // ratings 用户1~259137 电影1~165201
//        LMF=new LMFSetting(100000,100000,10000,10,
//                259137,165201,"data/LMFData/ratings"+workerId+".csv");

        LMF=new LMFSetting(470000,470000,47000,10,
                259137,165201,"data/LMFData/ratings"+workerId+".csv");


        switch (QuickSetting.algorithmName){
            case "LiR": generalSetting=LiR;break;
            case "LoR": generalSetting=LoR;break;
            case "SVM": generalSetting=SVM;break;
            case "LMF": break;
            default:System.out.println("无该算法");
        }
        // 其实有了上面的数据结构，下述的参数都可以省略，但是为了尽量少的改后面的程序，这里不去掉这些冗余了

//        sampleListSize=50000;
        levelDBPathForWorker="data/leveldbForWorker/";
        pruneRate=0.001f;
        inMemSampleBatchNum=100;

        minPartitionSize=2;

        if(!QuickSetting.algorithmName.equals("LMF")){
            sampleListSize=generalSetting.sampleListSize;
            samplePrunedSize=generalSetting.samplePrunedSize;
            myDataPath=generalSetting.myDataPath;
            sampleBatchSize=generalSetting.sampleBatchSize;
            catSize=generalSetting.catSize;
            sampleBatchListSize=generalSetting.sampleBatchListSize;
            sampleBatchListPrunedSize=generalSetting.sampleBatchListPrunedSize;
            kvStoreForLevelDB.init(levelDBPathForWorker,generalSetting.algorithmName);
        }else {
            samplePrunedSize_LMF=LMF.samplePrunedSize;
            myDataPath_LMF=LMF.dataPath;
            userNum_LMF=LMF.userNum;
            movieNum_LMF=LMF.movieNum;
            sampleListSize_LMF=LMF.sampleListSize; // 训练集的样本个数
            samplePrunedSize_LMF=LMF.samplePrunedSize;
            sampleBatchSize_LMF=LMF.sampleBatchSize;  // 一个batch的大小
            r_LMF=LMF.r;
            sampleBatchListPrunedSize_LMF=LMF.sampleBatchListPrunedSize;
            sampleBatchListSize_LMF=LMF.sampleBatchListSize; // 训练集包含batch的数目
            kvStoreForLevelDB.init(levelDBPathForWorker,"LMF");
        }




//        psWorker=new PSWorker(Context.serverIp.get(Context.masterId),Context.serverPort.get(Context.masterId));
        psRouterClient=new PSRouterClient();

    }
}
