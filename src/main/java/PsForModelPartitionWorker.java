import Algotithm.LogisticRegression;
import Util.*;
import context.Context;
import context.WorkerContext;
import javafx.concurrent.Worker;
import net.PSWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import store.KVStoreForLevelDB;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static Partitioning.data.DataPartitioning.dataPartitioning;

/**
 * @program: simplePsForModelPartition
 * @description: worker
 * @author: SongZhen
 * @create: 2018-12-07 10:45
 */
public class PsForModelPartitionWorker {

    public static void main(String[] args) throws IOException,ClassNotFoundException,InterruptedException {
        Logger logger=LoggerFactory.getLogger(PsForModelPartitionWorker.class.getName());
        MemoryUtil.showFreeMemory("start:");
        Context.init();
        WorkerContext.init();
        MemoryUtil.showFreeMemory("Context");


//        TestNetWork.test();

        // 将原始数据处理成one-hot编码的数据，然后存储在kv数据库中

        DataProcessUtil.metaToDB(WorkerContext.myDataPath,Context.featureSize,WorkerContext.catSize);



        // 获取稀疏的维度个数，并发送给自己的本地服务器
        if(Context.masterId==WorkerContext.workerId){
            Context.sparseDimSize=WorkerContext.psRouterClient.getLocalhostPSWorker().getSparseDimSize();
        }else {
            Context.sparseDimSize=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId) .getSparseDimSize();
        }
        logger.info("sparseDimSize:"+Context.sparseDimSize);



        // 规范化连续feature属性
        DataProcessUtil.linerNormalization();


        // 将稀疏维度的大小发给本地server，然后初始化参数
        WorkerContext.psRouterClient.getLocalhostPSWorker().sentSparseDimSizeAndInitParams(Context.sparseDimSize);


        // 上面的函数是参数在server的kvStore初始化的，但是在初始化前，应该先进行参数的划分
        PartitionUtil.partitionV();



        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();

        // 开始训练
        LogisticRegression logisticRegression=new LogisticRegression(0.001f,0.01f,50);
        MemoryUtil.releaseMemory();
        logisticRegression.train();






        WorkerContext.psRouterClient.shutdownAll();
        WorkerContext.kvStoreForLevelDB.getDb().close();


//        worker.pushKeyValueMap();
    }
}