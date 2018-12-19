import Util.DataProcessUtil;
import Util.PruneUtil;
import context.Context;
import context.WorkerContext;
import javafx.concurrent.Worker;
import net.PSWorker;
import store.KVStoreForLevelDB;

import java.io.IOException;

import static Partitioning.data.DataPartitioning.dataPartitioning;

/**
 * @program: simplePsForModelPartition
 * @description: worker
 * @author: SongZhen
 * @create: 2018-12-07 10:45
 */
public class PsForModelPartitionWorker {
    public static void main(String[] args) throws IOException,ClassNotFoundException,InterruptedException {
        Context.init();
        WorkerContext.init();

        // 将原始数据处理成one-hot编码的数据，然后存储在kv数据库中
        DataProcessUtil.metaToDB(WorkerContext.myDataPath,WorkerContext.featureSize,WorkerContext.catSize);

        // 获取稀疏的维度个数，并发送给自己的本地服务器
        if(Context.masterId==WorkerContext.workerId){
            Context.sparseDimSize=WorkerContext.psRouterClient.getLocalhostPSWorker().getSparseDimSize();
        }else {
            Context.sparseDimSize=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId) .getSparseDimSize();
        }

        // 规范化连续feature属性


        // 将稀疏维度的大小发给本地server，然后初始化参数
        WorkerContext.psRouterClient.getLocalhostPSWorker().sentSparseDimSizeAndInitParams(Context.sparseDimSize);

        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();




        WorkerContext.psRouterClient.shutdownAll();
        WorkerContext.kvStoreForLevelDB.getDb().close();


//        worker.pushKeyValueMap();
    }
}