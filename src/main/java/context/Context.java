package context;

import Util.DataProcessUtil;
import net.PSWorker;
import store.KVStoreForLevelDB;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description: 全局常亮存储在这个类中
 * @author: SongZhen
 * @create: 2018-12-02 18:19
 */
public class Context {
    /** 网络通信server的相关配置*/
    public static Map<String,String> serverAddress=new HashMap<String, String>();

    /** 判断是否是服务器*/
    public static boolean isPServer;
    public static String currentServerIp;
    public static String currentServerPort;

    /** 要对数据集进行划分*/
    public static int partitionedDataSize;
    public static int dataPartitionNum;


    /** worker和server数量*/
    public static int workerNum;
    public static int serverNum;

    /** 判断是否已经初始化过了*/
    public static boolean inited=false;

    /** 判断是分布式执行还是单机执行*/
    public static enum Mode{
        STANDALONE, DISTRIBUTED
    }
    public static Mode mode;

    /** 判断是不是server的master机器，管参数分配的*/
    public static boolean isMaster;


    /** 判断是否异步*/
    public static boolean isAsy=false;

    /** 当前是几号机器*/
    public static int workerId;

    /** 是否使用频率下限进行剪枝*/
    public static boolean usePruneRate;

    /** 剪枝率以及剪枝下限频率*/
    public static float pruneRate;
    public static float freqThreshold;

    /** 用于划分的数据采样个数*/
    public static int samplePrunedSize;

    /** 本地levelDB的地址*/
    public static String levelDBPath;

    public static String dataPath;

    /** cat是否在feature前面*/
    public static boolean isCatForwardFeature;

    /** 样本个数*/
    public static int sampleListSize;

    /** 所有的batch数目*/
    public static int sampleBatchListSize;

    public static int sampleBatchListPrunedSize;

    public static KVStoreForLevelDB kvStoreForLevelDB=new KVStoreForLevelDB();

    public static int featureSize;
    public static int catSize;
    public static int sampleBatchSize;
    public static int inMemSampleBatchNum;

    public static int minPartitionSize;
    public static long sparseDimSize;
    public static String myDataPath;

    public static String masterIp;
    public static String masterPort;

    public static boolean isDist;


    /** 剪枝后的batch的个数*/
    public static int sampleBatchPrunedNum;

    public static void init() throws IOException,ClassNotFoundException {
        if (inited){
            return;
        }
        inited=true;


        serverAddress.put("firstServerIp","202.199.6.30");
        serverAddress.put("firstServerPort","8999");
        serverAddress.put("secondServerIp","172.20.10.3");
        serverAddress.put("secondServerPort","8999");
//        serverAddress.put("thirdServerIp","172.20.10.13");
//        serverAddress.put("thirdServerPort","8999");

        masterIp="localhost";
        masterPort="8999";
        isPServer=true;
        isMaster=true;
        currentServerIp="firstServerIp";
        currentServerPort="firstServerPort";

        isDist=true;


        workerNum=2;
        serverNum=2;

        workerId=1;

        mode=Mode.DISTRIBUTED;

        usePruneRate=true;

        samplePrunedSize=50000;

        pruneRate=0.001f;
        freqThreshold=100;

        levelDBPath="data/leveldb/";
        dataPath="data/train.csv/";
        myDataPath="data/train"+workerId+".csv";

        isCatForwardFeature=true;
        sampleListSize=50000;

        kvStoreForLevelDB.init(levelDBPath);

        featureSize=10;
        catSize=12;

        sampleBatchSize=200;
        inMemSampleBatchNum=100;

        minPartitionSize=2;


        dataPartitionNum=workerNum;
        partitionedDataSize=100000;




        sampleBatchListSize=sampleListSize/sampleBatchSize;
        sampleBatchListPrunedSize=samplePrunedSize/sampleBatchSize;





    }
}