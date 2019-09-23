package data.preprocess;

import Util.DataProcessUtil;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import net.PServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-09-23 13:35
 */
public class OtherMetaToLevelDB {
    public static void main(String[] args) throws IOException,ClassNotFoundException {
        Logger logger = LoggerFactory.getLogger(OtherMetaToLevelDB.class);
        Context.init();
        ServerContext.init();
        WorkerContext.init();
        AtomicBoolean isFinishOneHot=new AtomicBoolean(false);

        // 给server开一个线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("start a Meta server thread");
                    // 当前server的端口号
                    PServer pServer = new PServer(Context.serverPort.get(ServerContext.serverId));
                    pServer.start();

                    while(true){
                        if(isFinishOneHot.get()){
                            pServer.stop();
                            ServerContext.kvStoreForLevelDB.getDb().close();
                            WorkerContext.kvStoreForLevelDB.getDb().close();
                            break;
                        }else {
                            Thread.sleep(1000);
                        }
                    }

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

        isFinishOneHot.set(true);



    }
}