package context;

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





    public static void init()throws IOException {

        workerId=0;
        mode=Mode.DISTRIBUTED;
        isCatForwardFeature=true;
//        sampleListSize=50000;
        sampleListSize=30;



//        samplePrunedSize=50000;
        samplePrunedSize=5;

        pruneRate=0.001f;

        // 下面是逻辑回归任务的数据集
//        myDataPath="data/train"+workerId+".csv";
        // 下面是线性回归任务
//        myDataPath="data/linearRegressionData/Salary_Data"+workerId+".csv";
        // 下面是低秩矩阵分解任务
        myDataPath="data/LMFData/LMFData"+workerId+".csv";


//        catSize=12;
        catSize=2;
//        sampleBatchSize=50000;
        sampleBatchSize=30;
        inMemSampleBatchNum=100;

        minPartitionSize=2;
        userNum_LMF=943;
        movieNum_LMF=2000;



        sampleBatchListSize=sampleListSize/sampleBatchSize;
        sampleBatchListPrunedSize=samplePrunedSize/sampleBatchSize;

        sampleListSize_LMF=30000; // 训练集的样本个数
        samplePrunedSize_LMF=30000;
        sampleBatchSize_LMF=3000;  // 一个batch的大小
        r_LMF=10;
        sampleBatchListPrunedSize_LMF=samplePrunedSize_LMF/sampleBatchSize_LMF;
        sampleBatchListSize_LMF=sampleListSize_LMF/sampleBatchSize_LMF; // 训练集包含batch的数目

        levelDBPathForWorker="data/leveldbForWorker/";
        kvStoreForLevelDB.init(levelDBPathForWorker);


//        psWorker=new PSWorker(Context.serverIp.get(Context.masterId),Context.serverPort.get(Context.masterId));
        psRouterClient=new PSRouterClient();

    }
}
