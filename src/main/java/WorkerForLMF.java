import Algotithm.LMF;
import Algotithm.LinearRegression;
import Util.CurrentTimeUtil;
import Util.DataProcessUtil;
import Util.MemoryUtil;
import context.Context;
import context.WorkerContext;
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
 * @create: 2019-05-13 15:23
 */
public class WorkerForLMF {
    public static void main(String[] args) throws IOException,ClassNotFoundException,InterruptedException {
        Logger logger=LoggerFactory.getLogger(WorkerForLinearRegression.class);
        Context.init();
        WorkerContext.init();

        // 先处理数据并放到key-value数据库中
        // 处理完的key是batchLMF0,value是matrix类型的
        DataProcessUtil.metaToDB_LMF();

        // 需要先对参数维度进行划分
        Set[] vSet= ParamPartition.partitionV_LMF();

        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();
        if (WorkerContext.workerId != Context.masterId) {
            LSetListArrayMessage ls_partitionedVSet = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getLsPartitionedVSet();
            WorkerContext.psRouterClient.getPsWorkers().get(WorkerContext.workerId).putLsPartitionedVSet(ls_partitionedVSet);
        }
        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();

        WorkerContext.kvStoreForLevelDB.setVSet(vSet);

        // 为LMF算法初始化server的参数
        WorkerContext.psRouterClient.getLocalhostPSWorker().sentSparseDimSizeAndInitParams_LMF(WorkerContext.userNum_LMF,WorkerContext.movieNum_LMF,WorkerContext.r_LMF, vSet);

        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();

        // 开始训练
        LMF lmf = new LMF(0.1f,0.001f,10,WorkerContext.r_LMF,WorkerContext.userNum_LMF,WorkerContext.movieNum_LMF);
        MemoryUtil.releaseMemory();

        CurrentTimeUtil.setStartTime();
        lmf.train();
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("train_Time");


        WorkerContext.psRouterClient.shutdownAll();
        WorkerContext.kvStoreForLevelDB.getDb().close();

    }
}