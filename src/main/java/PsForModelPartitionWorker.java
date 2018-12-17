import Util.DataProcessUtil;
import Util.PruneUtil;
import context.Context;
import context.WorkerContext;
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
        DataProcessUtil.metaToDB(Context.myDataPath,Context.featureSize,Context.catSize);

        // 获取稀疏的维度个数，并发送给自己的本地服务器
        Context.sparseDimSize=WorkerContext.psWorker.getSparseDimSize();
        System.out.println(Context.sparseDimSize);

        WorkerContext.psWorker.shutdown();
        Context.kvStoreForLevelDB.getDb().close();


//        worker.pushKeyValueMap();
    }
}