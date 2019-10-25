import Algotithm.LogisticRegression;
import Util.*;
import context.Context;
import context.ServerContext;

import context.WorkerContext;

import javafx.concurrent.Worker;
import net.BMessage;
import net.LSetListArrayMessage;
import net.PServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import paramPartition.ParamPartition;


import java.io.IOException;

import java.util.List;
import java.util.Set;


/**
 * @program: simplePsForModelPartition
 * @description: worker
 * @author: SongZhen
 * @create: 2018-12-07 10:45
 */
public class WorkerForLoR {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(WorkerForLoR.class);
        Context.init();
        ServerContext.init();
        WorkerContext.init();
        WorkerContext.kvStoreForLevelDB.setVSet((Set[] )TypeExchangeUtil.toObject(WorkerContext.kvStoreForLevelDB.getDb().get("vSet".getBytes())));

        // 给server开一个线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("start a LoR server thread");
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





        // 开始训练
        LogisticRegression logisticRegression = new LogisticRegression(0.001f, 0.01f, 10);
        MemoryUtil.releaseMemory();

        long start_train=System.currentTimeMillis();
        logisticRegression.train();
        long end_train=System.currentTimeMillis();
        System.out.println("训练时间："+(end_train-start_train));

        WorkerContext.psRouterClient.getLocalhostPSWorker().getBlockingStub().showSomeStatisticAfterTrain(BMessage.newBuilder().setB(true).build());



        WorkerContext.psRouterClient.shutdownAll();
        WorkerContext.kvStoreForLevelDB.getDb().close();


//        worker.pushKeyValueMap();
    }
}
