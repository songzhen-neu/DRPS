import Algotithm.LMF;
import Algotithm.LinearRegression;
import Util.CurrentTimeUtil;
import Util.DataProcessUtil;
import Util.MemoryUtil;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import net.BMessage;
import net.LSetListArrayMessage;
import net.PServer;
import org.iq80.leveldb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import paramPartition.ParamPartition;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-13 15:23
 */
public class WorkerForLMF {
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(WorkerForLinearRegression.class);
        Context.init();
        ServerContext.init();
        WorkerContext.init();


        // 给server开一个线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("start a server thread");
                    // 当前server的端口号
                    PServer pServer = new PServer(Context.serverPort.get(ServerContext.serverId));
                    pServer.start();
                    pServer.blockUntilShutdown();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();


        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 这个位置需要全局等一等，也就是所有server都准备好的之后才进行后面的工作
        Context.psRouterClient.getPsWorkers().get(Context.masterId).serverSynchronization();


//        Context.init();


        // 先处理数据并放到key-value数据库中
        // 处理完的key是batchLMF0,value是matrix类型的
//        DataProcessUtil.metaToDB_LMF();

        // 需要先对参数维度进行划分
        long start = System.currentTimeMillis();
        Set[] vSet = ParamPartition.partitionV_LMF();
        long end = System.currentTimeMillis();


        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();
        if (WorkerContext.workerId != Context.masterId) {
            LSetListArrayMessage ls_partitionedVSet = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getLsPartitionedVSet();
            WorkerContext.psRouterClient.getPsWorkers().get(WorkerContext.workerId).putLsPartitionedVSet(ls_partitionedVSet);
        }
        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();

        WorkerContext.kvStoreForLevelDB.setVSet(vSet);

        // 为LMF算法初始化server的参数
        WorkerContext.psRouterClient.getLocalhostPSWorker().sentSparseDimSizeAndInitParams_LMF(WorkerContext.userNum_LMF, WorkerContext.movieNum_LMF, WorkerContext.r_LMF, vSet);

        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();

        // 开始训练
        LMF lmf = new LMF(0.1f, 0.01f, 30, WorkerContext.r_LMF, WorkerContext.userNum_LMF, WorkerContext.movieNum_LMF);
        MemoryUtil.releaseMemory();

        long start_train=System.currentTimeMillis();
        lmf.train();
        long end_train=System.currentTimeMillis();
        logger.info("训练时间："+(end_train-start_train));
        logger.info("建立索引的时间为:" + (end - start));

        WorkerContext.psRouterClient.getLocalhostPSWorker().getBlockingStub().showSomeStatisticAfterTrain(BMessage.newBuilder().setB(true).build());
        WorkerContext.psRouterClient.shutdownAll();
        WorkerContext.kvStoreForLevelDB.getDb().close();

    }
}