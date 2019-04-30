package parallelism;

import context.Context;
import context.ServerContext;
import dataStructure.enumType.ParallelismControlModel;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.ConcurrentSet;
import net.IMessage;
import net.SFKVListMessage;
import net.ServerIdAndWorkerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-04-17 21:04
 */
public class SSP {
    /**
     * @Description: 这里SSP是每一个worker有各自的时钟，SSPS是只要pull了之后，时钟就统一
     * @Param:
     * @return:
     * @Author: SongZhen
     * @date: 上午10:55 19-4-22
     */
    public static ConcurrentSet[] barrier;
    public static AtomicInteger[] count;
    public static AtomicInteger[] iteration;
    public static AtomicBoolean isInited = new AtomicBoolean(false);
    public static final int bound = Context.boundForSSP;
    public static AtomicBoolean[] isContains;
    public static Logger logger = LoggerFactory.getLogger(SSP.class);
    public static AtomicBoolean[] isWaiting;
    public static AtomicInteger[] curIterationOfWorker;
    public static AtomicBoolean[] barrierForOtherServer;
    public static AtomicBoolean[] barrierIsWaiting;


    public static void init() {
        synchronized (isInited) {
            if (!isInited.getAndSet(true)) {
                barrier = new ConcurrentSet[Context.workerNum];
                count = new AtomicInteger[Context.workerNum];
                iteration = new AtomicInteger[Context.workerNum];
                isContains = new AtomicBoolean[Context.workerNum];
                isWaiting = new AtomicBoolean[Context.workerNum];
                curIterationOfWorker = new AtomicInteger[Context.workerNum];
                barrierForOtherServer = new AtomicBoolean[Context.workerNum];
                barrierIsWaiting = new AtomicBoolean[Context.workerNum];

                for (int i = 0; i < Context.workerNum; i++) {
                    barrier[i] = new ConcurrentSet();
                    count[i] = new AtomicInteger(0);
                    iteration[i] = new AtomicInteger(0);
                    isContains[i] = new AtomicBoolean(false);
                    isWaiting[i] = new AtomicBoolean(false);
                    curIterationOfWorker[i] = new AtomicInteger(0);
                    barrierForOtherServer[i] = new AtomicBoolean(false);
                    barrierIsWaiting[i] = new AtomicBoolean(false);
                }
            }


        }

    }


    public static void isRespOrWaited(int workerId, StreamObserver<SFKVListMessage> resp, Set<String> neededParamIndices, int iterationOfWi) {
        // 如果当前worker被其他worker等待，那么其他worker计数+1，并判断是否要notify
        // worker同时只能有一个进入，因为如果一起进入的话，可能worker同时wait
        System.out.println(iterationOfWi + ",worker" + workerId + " in");
        curIterationOfWorker[workerId].set(iterationOfWi);
        if (Context.workerNum > 1) {
            if (ServerContext.serverId == Context.masterId) {
                // 先同步，让master进程wait其他server
                // 做三个worker的同步异步问题
                RespTool.waitForNonMasterServerWaiting(workerId, isWaiting);



                synchronized (barrier) {
                    System.out.println(iterationOfWi + ",worker" + workerId + " barrier in");
                    // 判断当workerId执行完后，判断workerId是否被其他worker等待
                    // 如果被等待，count++，判断是否通知等待的worker继续执行，如果不被等待，执行WSP
                    for (int j = 0; j < barrier.length; j++) {
                        if (barrier[j].contains(workerId)) {
                            count[j].incrementAndGet();
                            if (count[j].get() == barrier[j].size()) {
                                while (!barrierIsWaiting[j].get()){
                                    try{
                                        Thread.sleep(10);
                                    }catch (InterruptedException e){
                                        e.printStackTrace();
                                    }
                                }
                                synchronized (barrier[j]) {
                                    barrier[j].notifyAll();
                                }
                                isContains[workerId].set(true);
                            }
                        }
                    }




                    if (!isContains[workerId].get()) {
                        // 把所有迭代次数小于iteration[workerId]-2的进程全部加入barrier里
                        for (int i = 0; i < iteration.length; i++) {
                            if (i != workerId && curIterationOfWorker[i].get() < Context.trainRoundNum.get()) {
                                if (Context.parallelismControlModel == ParallelismControlModel.SSP_S) {
                                    if (iteration[i].get() <= getMaxIteration(iteration) + 1 - bound) {
                                        barrier[workerId].add(i);
//                                logger.info("worker:" + workerId + ",wait:" + i);
                                    }
                                } else if (Context.parallelismControlModel == ParallelismControlModel.SSP) {
                                    if (iteration[i].get() <= iteration[workerId].get() + 1 - bound) {
                                        barrier[workerId].add(i);
//                                logger.info("worker:" + workerId + ",wait:" + i);
                                    }
                                }

                            }

                        }
                    }
                }

                System.out.println(iterationOfWi + ",worker" + workerId + " barrier out");

                for (int j = 0; j < barrier.length; j++) {
                    for (Object i : barrier[j]) {
                        System.out.println(j + ":" + (Integer) i);
                    }
                }


                if (barrier[workerId].size() != 0 && !isContains[workerId].get()) {
                    try {
//                            logger.info(workerId + ":" + "4");
                        synchronized (barrier[workerId]) {
                            System.out.println(iterationOfWi + "," + workerId + ":" + "begin");
                            barrierIsWaiting[workerId].set(true);
                            barrier[workerId].wait();
                            System.out.println(iterationOfWi + "," + workerId + ":" + "end");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                if (Context.parallelismControlModel == ParallelismControlModel.SSP_S) {
                    iteration[workerId].set(getMaxIteration(iteration) + 1);
                } else if (Context.parallelismControlModel == ParallelismControlModel.SSP) {
                    iteration[workerId].incrementAndGet();
                }

                barrier[workerId].clear();
                count[workerId].set(0);
                barrierIsWaiting[workerId].set(false);
                synchronized (isContains[workerId]){
                    isContains[workerId].set(false);
                }



                for (int i = 0; i < Context.serverNum; i++) {
                    if (i != Context.masterId) {
//                        System.out.println("已经notify其他的了");
                        Context.psRouterClient.getPsWorkers().get(i).getBlockingStub().notifyForSSP(IMessage.newBuilder().setI(workerId).build());
                    }
                }
                RespTool.respParam(resp, neededParamIndices);


            } else {
                // 做其他两个server的同步异步问题

                FutureTask<Boolean> task = new FutureTask<>(() -> {
                    try {

                        System.out.println("bbba");
                        barrierForOtherServer[workerId].set(true);
                        barrierForOtherServer[workerId].wait();
                        System.out.println("aaa");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, Boolean.TRUE);
                synchronized (barrierForOtherServer[workerId]) {
                    new Thread(task).start();
                }

                // 保证了一定进入了上面对barrierForOtherServer[workerId]的锁

                    while (!barrierForOtherServer[workerId].get()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                synchronized (barrierForOtherServer[workerId]) {
                    barrierForOtherServer[workerId].set(false);
                }
                // 这样可以保证只有在上面锁释放的时候，才能通知master，该进程在等待
                Context.psRouterClient.getPsWorkers().get(Context.masterId).getBlockingStub().isWaiting(ServerIdAndWorkerId.newBuilder()
                        .setWorkerId(workerId)
                        .setServerId(ServerContext.serverId)
                        .build());
                // 等待master的notify
                while (!task.isDone()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                RespTool.respParam(resp, neededParamIndices);
            }
        } else {
            RespTool.respParam(resp, neededParamIndices);
        }


    }




    public static int getMaxIteration(AtomicInteger[] iteration) {
        int maxValue = Integer.MIN_VALUE;
        for (int i = 0; i < iteration.length; i++) {
            if (iteration[i].get() > maxValue) {
                maxValue = iteration[i].get();
            }
        }
        return maxValue;
    }


}