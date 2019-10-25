package paramPartition;

import Util.TypeExchangeUtil;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import net.LSetListArrayMessage;
import net.PServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-10-03 15:27
 */
public class LoRParamConstruct {
    public static void main(String[] args)throws IOException,ClassNotFoundException{
        Logger logger = LoggerFactory.getLogger(LoRParamConstruct.class);
        Context.init();
        ServerContext.init();
        WorkerContext.init();
        AtomicBoolean isFinishParamInit=new AtomicBoolean(false);

        // 给server开一个线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("start a LoRParaConstruct server thread");
                    // 当前server的端口号
                    PServer pServer = new PServer(Context.serverPort.get(ServerContext.serverId));
                    pServer.start();
                    while(true){
                        if(isFinishParamInit.get()){
                            System.out.println("LoR finish param init");
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



        // 上面的函数是参数在server的kvStore初始化的，但是在初始化前，应该先进行参数的划分
        long start=System.currentTimeMillis();
        Set[] vSet=ParamPartition.partitionV();
        long end=System.currentTimeMillis();

        WorkerContext.kvStoreForLevelDB.getDb().put("vSet".getBytes(),TypeExchangeUtil.toByteArray(vSet));




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
        WorkerContext.psRouterClient.getLocalhostPSWorker().sentSparseDimSizeAndInitParams(WorkerContext.generalSetting.sparseDimSize, vSet);
//        WorkerContext.psRouterClient.getLocalhostPSWorker().sentSparseDimSizeAndInitParams(Context.sparseDimSize);


        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();
        logger.info("建立索引的时间为"+(end-start));
        isFinishParamInit.set(true);
    }
}