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

        workerId=0;
        mode=Mode.DISTRIBUTED;
        isCatForwardFeature=true;
        LiR=new GeneralAlgorithmSetting(450,450,50,
                "data/LiRData/housing"+workerId+".csv",9,0);
        LoR=new GeneralAlgorithmSetting(1000,1000,100,
                "data/LoRData/train"+workerId+".csv",12,10);
        SVM=new GeneralAlgorithmSetting(450,450,50,
                "data/SVMData/dataset"+workerId+".csv",3,4);

        // 用户1～53424，电影1~10000，样本数量5976479
        LMF=new LMFSetting(19000,19000,19000,10,
                53424,10000,"data/LMFData/Goodbooks"+workerId+".csv");

        generalSetting=LoR;
        // 其实有了上面的数据结构，下述的参数都可以省略，但是为了尽量少的改后面的程序，这里不去掉这些冗余了

//        sampleListSize=50000;
        sampleListSize=generalSetting.sampleListSize;



//        samplePrunedSize=50000;
        samplePrunedSize=generalSetting.samplePrunedSize;
        samplePrunedSize_LMF=LMF.samplePrunedSize;
        pruneRate=0.001f;

        // 下面是逻辑回归任务的数据集
//        myDataPath="data/train"+workerId+".csv";
        // 下面是线性回归任务
//        myDataPath="data/linearRegressionData/Salary_Data"+workerId+".csv";
        // 下面是低秩矩阵分解任务
//        myDataPath="data/LMFData/LMFData"+workerId+".csv";
        // 下面是SVM的数据集
        myDataPath=generalSetting.myDataPath;
        myDataPath_LMF=LMF.dataPath;

//        catSize=12;
//        catSize=2;
        // 下面是SVM的catSize
        catSize=generalSetting.catSize;

//        sampleBatchSize=50000;
        sampleBatchSize=generalSetting.sampleBatchSize;
        inMemSampleBatchNum=100;

        minPartitionSize=2;
        userNum_LMF=LMF.userNum;
        movieNum_LMF=LMF.movieNum;



        sampleBatchListSize=generalSetting.sampleBatchListSize;
        sampleBatchListPrunedSize=generalSetting.sampleBatchListPrunedSize;

        sampleListSize_LMF=LMF.sampleListSize; // 训练集的样本个数
        samplePrunedSize_LMF=LMF.samplePrunedSize;
        sampleBatchSize_LMF=LMF.sampleBatchSize;  // 一个batch的大小
        r_LMF=10;
        sampleBatchListPrunedSize_LMF=LMF.sampleBatchListPrunedSize;
        sampleBatchListSize_LMF=LMF.sampleBatchListSize; // 训练集包含batch的数目

        levelDBPathForWorker="data/leveldbForWorker/";
        kvStoreForLevelDB.init(levelDBPathForWorker);


//        psWorker=new PSWorker(Context.serverIp.get(Context.masterId),Context.serverPort.get(Context.masterId));
        psRouterClient=new PSRouterClient();

    }
}
