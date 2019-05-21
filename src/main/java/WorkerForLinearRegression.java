import Algotithm.LinearRegression;
import Algotithm.LogisticRegression;
import Util.CurrentTimeUtil;
import Util.DataProcessUtil;
import Util.MemoryUtil;
import context.Context;
import context.WorkerContext;
import net.BMessage;
import net.LSetListArrayMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import paramPartition.ParamPartition;

import java.io.IOException;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-13 10:41
 */
public class WorkerForLinearRegression {
    public static void main(String[] args) throws IOException,ClassNotFoundException,InterruptedException {
        Logger logger=LoggerFactory.getLogger(WorkerForLinearRegression.class);

        Context.init();
        WorkerContext.init();

        DataProcessUtil.metaToDB(WorkerContext.myDataPath, Context.featureSize, WorkerContext.catSize);


        // 获取稀疏的维度个数，并发送给自己的本地服务器
        if (Context.masterId == WorkerContext.workerId) {
            Context.sparseDimSize = WorkerContext.psRouterClient.getLocalhostPSWorker().getSparseDimSize();
        } else {
            Context.sparseDimSize = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getSparseDimSize();
        }

        logger.info("sparseDimSize:" + Context.sparseDimSize);


        // 规范化连续feature属性
        logger.info("linearNormalization start");
        DataProcessUtil.linerNormalization();
        logger.info("linearNormalization end");


        // 上面的函数是参数在server的kvStore初始化的，但是在初始化前，应该先进行参数的划分
//        Set[] vSet = PartitionUtil.partitionV();
        long start=System.currentTimeMillis();
        Set[] vSet=ParamPartition.partitionV();
        long end =System.currentTimeMillis();

//        Set[] vSet=SetUtil.initSetArray(Context.serverNum);
        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();
        if (WorkerContext.workerId != Context.masterId) {
            LSetListArrayMessage ls_partitionedVSet = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getLsPartitionedVSet();
            WorkerContext.psRouterClient.getPsWorkers().get(WorkerContext.workerId).putLsPartitionedVSet(ls_partitionedVSet);
        }
        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();

        WorkerContext.kvStoreForLevelDB.setVSet(vSet);


        // 需要获取一下ls_partitionedVSet，然后传递给每一个本地服务器


        // 根据vSet重新分配一下参数，这些维度在vSet里找，其他维度按照取余的方式
        // 将稀疏维度的大小发给本地server，然后初始化参数,这里如果vSet是空，也就是freqThreshold非常大，就相当于没划分了，所以不用单独写一个函数了
        WorkerContext.psRouterClient.getLocalhostPSWorker().sentSparseDimSizeAndInitParams(Context.sparseDimSize, vSet);
//        WorkerContext.psRouterClient.getLocalhostPSWorker().sentSparseDimSizeAndInitParams(Context.sparseDimSize);


        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();

        // 开始训练
        LinearRegression linearRegression = new LinearRegression(0.001f, 0.01f, 100);
        MemoryUtil.releaseMemory();

        CurrentTimeUtil.setStartTime();
        linearRegression.train();
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("训练时间为");
        WorkerContext.psRouterClient.getLocalhostPSWorker().getBlockingStub().showSomeStatisticAfterTrain(BMessage.newBuilder().setB(true).build());
        logger.info("索引建立时间为:"+(end-start));

        WorkerContext.psRouterClient.shutdownAll();
        WorkerContext.kvStoreForLevelDB.getDb().close();
    }
}