package parallelism;

import context.Context;
import context.ServerContext;
import context.WorkerContext;
import dataStructure.parallelismControlModel.IterationTimeTable;
import dataStructure.parallelismControlModel.StrategyChoiceTable;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.ConcurrentSet;
import lombok.Data;
import lombok.Synchronized;
import net.IMessage;
import net.SFKVListMessage;
import net.SRListMessage;
import net.ServerIdAndWorkerId;

import java.io.IOException;
import java.sql.Time;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: simplePsForModelPartition
 * @description: worker-selection control model
 * @author: SongZhen
 * @create: 2019-04-09 14:45
 */
@Data
public class WSP {
    // 下面开始定义WSP用到的数据结构
    /**
     * 迭代时间表定义，每个server中都存有大小为workerNum的迭代时间表
     */
    public static IterationTimeTable[] iTTableArray = new IterationTimeTable[Context.workerNum];
    public static StrategyChoiceTable[] sCTArray = new StrategyChoiceTable[Context.workerNum];
    /**
     * 用来记录当前已经到达的需要的worker的数量
     */
    public static AtomicInteger[] count = new AtomicInteger[Context.workerNum];
    /**
     * 记录WSP中，worker j需要等待完成的所有worker i
     */
    public static ConcurrentSet[] optimalPlanSet = new ConcurrentSet[Context.workerNum];
    /**
     * 每一个worker都可能选择一个方案，每个worker选择的方案都需要对选择数据进行同步
     */
    public static AtomicBoolean[] barrier_forWSP = new AtomicBoolean[Context.workerNum];
    public static AtomicBoolean isInited = new AtomicBoolean(false);
    public static AtomicInteger[] curIterationOfWorker = new AtomicInteger[Context.workerNum];

    /**
     * 针对非master机器的
     */
    public static AtomicBoolean[] WSP_WaitBarrier = new AtomicBoolean[Context.workerNum];
    /**
     * master机器的
     */
    public static AtomicBoolean[] WSP_IsWaiting = new AtomicBoolean[Context.workerNum];
    /**
     * 需要等待barrier等待了之后，才能notify
     */
    public static AtomicBoolean[] WSP_BarrierIsWaiting = new AtomicBoolean[Context.workerNum];

    @Synchronized
    public static void init() {
        synchronized (isInited) {
            if (!isInited.getAndSet(true)) {
                // 没有初始化，那么执行以下代码
                for (int i = 0; i < iTTableArray.length; i++) {
                    iTTableArray[i] = new IterationTimeTable();
                }
                for (int i = 0; i < sCTArray.length; i++) {
                    sCTArray[i] = new StrategyChoiceTable();
                }
                for (int i = 0; i < optimalPlanSet.length; i++) {
                    optimalPlanSet[i] = new ConcurrentSet();
                }

                for (int i = 0; i < Context.workerNum; i++) {
                    count[i] = new AtomicInteger(0);
                    iTTableArray[i].startTime = System.currentTimeMillis();
                    iTTableArray[i].iteration = 0;
                    barrier_forWSP[i] = new AtomicBoolean(false);
                    curIterationOfWorker[i] = new AtomicInteger(0);
                    WSP_WaitBarrier[i] = new AtomicBoolean(false);
                    WSP_IsWaiting[i] = new AtomicBoolean(false);
                    WSP_BarrierIsWaiting[i] = new AtomicBoolean(false);
                    isContainedInOtherPlan[i] = new AtomicBoolean(false);
                    containedInId[i] = new AtomicInteger(-1);
                    isFirstItaration[i] = new AtomicBoolean(true);
                    isFinishIter[i]=new AtomicBoolean(false);
                }

            }
        }
    }

    public static AtomicBoolean isFinished = new AtomicBoolean(false);
    public static AtomicInteger barrier_forIsRespOrWaited = new AtomicInteger(0);
    public static AtomicBoolean[] isContainedInOtherPlan = new AtomicBoolean[Context.workerNum];
    public static AtomicBoolean[] isFirstItaration = new AtomicBoolean[Context.workerNum];
    public static CyclicBarrier cyclicBarrier = new CyclicBarrier(Context.workerNum);
    public static AtomicBoolean barrier = new AtomicBoolean(false);
    public static AtomicInteger[] containedInId = new AtomicInteger[Context.workerNum];
    public static AtomicLong maxIteratitionTime = new AtomicLong(0);
    public static AtomicBoolean[] isFinishIter=new AtomicBoolean[Context.serverNum];


    public static void isRespOrWaited(int workerId, StreamObserver<SFKVListMessage> resp, Set<String> neededParamIndices, int iterationOfWi) throws ClassNotFoundException, IOException, InterruptedException {
        if (Context.workerNum > 1) {
//            System.out.println(Context.trainRoundNum);
                if (ServerContext.serverId == Context.masterId) {
                    if(iterationOfWi==Context.trainRoundNum.get()){
                        isFinishIter[workerId].set(true);
                    }
                    isContainedInOtherPlan[workerId].set(false);
                    // 等待直到非master的所有server都等待master的指令才继续执行
                    RespTool.waitForNonMasterServerWaiting(workerId, WSP_IsWaiting);

                    // 如果是第一次迭代的话，那么需要同步一样返回，记录一下时间
                    if (isFirstItaration[workerId].get()) {
                        iTTableArray[workerId].endTime = System.currentTimeMillis();
                        iTTableArray[workerId].execTime = iTTableArray[workerId].endTime - iTTableArray[workerId].startTime;
                        isFirstItaration[workerId].set(false);
                        // 一次迭代的执行时间
                        RespTool.respParam(resp, neededParamIndices);

                        try {
                            cyclicBarrier.await();
                        } catch (BrokenBarrierException e) {
                            e.printStackTrace();
                        }
//                        if (iTTableArray[workerId].execTime > maxIteratitionTime.get()) {
//                            maxIteratitionTime.set(iTTableArray[workerId].execTime);
//                        }
                        maxIteratitionTime.set(iTTableArray[workerId].execTime/Context.workerNum);

                        iTTableArray[workerId].startTime = System.currentTimeMillis();
                        iTTableArray[workerId].endTime = iTTableArray[workerId].startTime + iTTableArray[workerId].execTime;
                        iTTableArray[workerId].iteration = 1;
                    } else {
                        synchronized (barrier) {
                            iTTableArray[workerId].endTime = System.currentTimeMillis();
                            iTTableArray[workerId].execTime = iTTableArray[workerId].endTime - iTTableArray[workerId].startTime;
                            // master统一控制所有server的同步异步，其他worker只需要等待即可
                            // 现在有一个bug，如果所有worker同时进这个函数发现都没包含，然后在同时进入另外一个函数需要互相等待，这样就死锁了
                            for (int i = 0; i < Context.workerNum; i++) {
                                // 这里是遍历有没有其他的worker i在等待workerId
                                if (optimalPlanSet[i].contains(workerId)) {
                                    count[i].incrementAndGet();
                                    if (optimalPlanSet[i].size() == count[i].get()) {
                                        while (!barrier_forWSP[i].get()) {
                                            try {
                                                Thread.sleep(1);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        synchronized (barrier_forWSP[i]) {
                                            // 这里notify的是包含workerId的 worker
                                            barrier_forWSP[i].notifyAll();
                                        }

                                    }
                                    // 只要被1个包含，就包含了，那么workerId就得等待
                                    // 必须所有都通知了，才能走
                                    isContainedInOtherPlan[workerId].set(true);
                                }
                            }


                            // 如果不包含在其他worker的执行方案里，那么需要进行策略选择
                            if (!isContainedInOtherPlan[workerId].get()) {
                                int maxIteration = getMaxIteration(iTTableArray);
                                for (int i = 0; i < Context.workerNum; i++) {
                                    // 判断workerId是否要等待i，i的开始时间+i的执行时间，即为i的理论上的结束时间，减去workerId的结束时间，相当于当前时间（上述内容表示需要等待i的时间）
                                    float waitTime = ((float) (iTTableArray[i].startTime + iTTableArray[i].execTime - iTTableArray[workerId].endTime) / maxIteratitionTime.get()) * Context.TimeMulWSP;
                                    if (waitTime > 0) {
                                        sCTArray[i].waitTime = waitTime;
                                    } else {
                                        sCTArray[i].waitTime = 0;
                                    }
                                    // workerId等待i的时候，staleness值,等待0意思是不等待
                                    if (i == workerId) {
                                        for (int j = 0; j < Context.workerNum; j++) {
                                            if (j != workerId) {
                                                sCTArray[workerId].staleness += (maxIteration + 1 - iTTableArray[j].iteration);
                                            }

                                        }
                                    } else {
                                        // 如果要等待i，那么
                                        for (int j = 0; j < Context.workerNum; j++) {
                                            if (j != workerId && j != i) {
                                                sCTArray[i].staleness += (maxIteration + 1 - iTTableArray[j].iteration);
                                            }
                                        }
                                    }
                                    sCTArray[i].negGain = sCTArray[i].waitTime + sCTArray[i].staleness;
                                }

                                optimalPlanSet[workerId] = getIOfMinNegGain(iTTableArray, sCTArray, workerId);
                                for(int i=0;i<Context.workerNum;i++){
                                    sCTArray[i].waitTime=0;
                                    sCTArray[i].staleness=0;
                                    sCTArray[i].negGain=0;
                                }

                            }
                        }


                        synchronized (barrier_forWSP[workerId]) {
                            barrier_forWSP[workerId].set(false);
                        }

                        if (!isContainedInOtherPlan[workerId].get() && optimalPlanSet[workerId].size() != 0) {
                            synchronized (barrier_forWSP[workerId]) {
                                barrier_forWSP[workerId].set(true);
                                barrier_forWSP[workerId].wait();
                            }
                        }


                        optimalPlanSet[workerId].clear();
                        count[workerId].set(0);

                        for (int j = 0; j < Context.serverNum; j++) {
                            if (j != Context.masterId) {
                                Context.psRouterClient.getPsWorkers().get(j).getBlockingStub().notifyForWSP(IMessage.newBuilder().setI(workerId).build());
                            }
                        }
                        RespTool.respParam(resp, neededParamIndices);
                        // 返回参数后，重新设置开始时间
                        iTTableArray[workerId].startTime = System.currentTimeMillis();
                        iTTableArray[workerId].iteration = getMaxIteration(iTTableArray) + 1;

                        // 返回完参数之后开始更新时间，如果不在上述要求线程（等待线程）里面，那么需要进行策略选择，
                        // 如果在的话，上面已经选择过等待还是继续了，当可以继续执行时需要更新当前的一写ITTable的信息


                    }


                } else {
                    // 等待master的通知（在等待之后发送消息给master）
                    FutureTask<Boolean> task = new FutureTask<>(() -> {
                        try {
                            WSP_WaitBarrier[workerId].set(true);
                            WSP_WaitBarrier[workerId].wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }, Boolean.TRUE);
                    synchronized (WSP_WaitBarrier[workerId]) {
                        new Thread(task).start();
                    }

                    // 保证了一定进入了上面对barrierForOtherServer[workerId]的锁

                    while (!WSP_WaitBarrier[workerId].get()) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    synchronized (WSP_WaitBarrier[workerId]) {
                        WSP_WaitBarrier[workerId].set(false);
                    }
                    // 这样可以保证只有在上面锁释放的时候，才能通知master，该进程在等待
                    Context.psRouterClient.getPsWorkers().get(Context.masterId).getBlockingStub().notifyNonMasterIsWaitingWSP(ServerIdAndWorkerId.newBuilder()
                            .setWorkerId(workerId)
                            .setServerId(ServerContext.serverId)
                            .build());
                    // 等待master的notify
                    while (!task.isDone()) {
                        try {
                            Thread.sleep(1);
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


    public static void isRespOrWaited_LMF(int workerId, StreamObserver<SRListMessage> resp, Set<String> neededParamIndices, int iterationOfWi) throws ClassNotFoundException, IOException, InterruptedException {
        if (Context.workerNum > 1) {
//            System.out.println(Context.trainRoundNum);
            if (ServerContext.serverId == Context.masterId) {
                if(iterationOfWi==Context.trainRoundNum.get()){
                    isFinishIter[workerId].set(true);
                }
                isContainedInOtherPlan[workerId].set(false);
                // 等待直到非master的所有server都等待master的指令才继续执行
                RespTool.waitForNonMasterServerWaiting(workerId, WSP_IsWaiting);

                // 如果是第一次迭代的话，那么需要同步一样返回，记录一下时间
                if (isFirstItaration[workerId].get()) {
                    iTTableArray[workerId].endTime = System.currentTimeMillis();
                    iTTableArray[workerId].execTime = iTTableArray[workerId].endTime - iTTableArray[workerId].startTime;
                    isFirstItaration[workerId].set(false);
                    // 一次迭代的执行时间
                    RespTool.respParam_LMF(resp, neededParamIndices);

                    try {
                        cyclicBarrier.await();
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                    if (iTTableArray[workerId].execTime > maxIteratitionTime.get()) {
                        maxIteratitionTime.set(iTTableArray[workerId].execTime);
                    }

                    iTTableArray[workerId].startTime = System.currentTimeMillis();
                    iTTableArray[workerId].endTime = iTTableArray[workerId].startTime + iTTableArray[workerId].execTime;
                    iTTableArray[workerId].iteration = 1;
                } else {
                    synchronized (barrier) {
                        iTTableArray[workerId].endTime = System.currentTimeMillis();
                        iTTableArray[workerId].execTime = iTTableArray[workerId].endTime - iTTableArray[workerId].startTime;
                        // master统一控制所有server的同步异步，其他worker只需要等待即可
                        // 现在有一个bug，如果所有worker同时进这个函数发现都没包含，然后在同时进入另外一个函数需要互相等待，这样就死锁了
                        for (int i = 0; i < Context.workerNum; i++) {
                            // 这里是遍历有没有其他的worker i在等待workerId
                            if (optimalPlanSet[i].contains(workerId)) {
                                count[i].incrementAndGet();
                                if (optimalPlanSet[i].size() == count[i].get()) {
                                    while (!barrier_forWSP[i].get()) {
                                        try {
                                            Thread.sleep(1);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    synchronized (barrier_forWSP[i]) {
                                        // 这里notify的是包含workerId的 worker
                                        barrier_forWSP[i].notifyAll();
                                    }

                                }
                                // 只要被1个包含，就包含了，那么workerId就得等待
                                // 必须所有都通知了，才能走
                                isContainedInOtherPlan[workerId].set(true);
                            }
                        }


                        // 如果不包含在其他worker的执行方案里，那么需要进行策略选择
                        if (!isContainedInOtherPlan[workerId].get()) {
                            int maxIteration = getMaxIteration(iTTableArray);
                            for (int i = 0; i < Context.workerNum; i++) {
                                // 判断workerId是否要等待i，i的开始时间+i的执行时间，即为i的理论上的结束时间，减去workerId的结束时间，相当于当前时间（上述内容表示需要等待i的时间）
                                float waitTime = ((float) (iTTableArray[i].startTime + iTTableArray[i].execTime - iTTableArray[workerId].endTime) / maxIteratitionTime.get()) * Context.TimeMulWSP;
                                if (waitTime > 0) {
                                    sCTArray[i].waitTime = waitTime;
                                } else {
                                    sCTArray[i].waitTime = 0;
                                }
                                // workerId等待i的时候，staleness值,等待0意思是不等待
                                if (i == workerId) {
                                    for (int j = 0; j < Context.workerNum; j++) {
                                        if (j != workerId) {
                                            sCTArray[workerId].staleness += (maxIteration + 1 - iTTableArray[j].iteration);
                                        }

                                    }
                                } else {
                                    // 如果要等待i，那么
                                    for (int j = 0; j < Context.workerNum; j++) {
                                        if (j != workerId && j != i) {
                                            sCTArray[workerId].staleness += (maxIteration + 1 - iTTableArray[j].iteration);
                                        }
                                    }
                                }
                                sCTArray[i].negGain = sCTArray[i].waitTime + sCTArray[i].staleness;
                            }

                            optimalPlanSet[workerId] = getIOfMinNegGain(iTTableArray, sCTArray, workerId);

                        }
                    }


                    synchronized (barrier_forWSP[workerId]) {
                        barrier_forWSP[workerId].set(false);
                    }

                    if (!isContainedInOtherPlan[workerId].get() && optimalPlanSet[workerId].size() != 0) {
                        synchronized (barrier_forWSP[workerId]) {
                            barrier_forWSP[workerId].set(true);
                            barrier_forWSP[workerId].wait();
                        }
                    }


                    optimalPlanSet[workerId].clear();
                    count[workerId].set(0);

                    for (int j = 0; j < Context.serverNum; j++) {
                        if (j != Context.masterId) {
                            Context.psRouterClient.getPsWorkers().get(j).getBlockingStub().notifyForWSP(IMessage.newBuilder().setI(workerId).build());
                        }
                    }
                    RespTool.respParam_LMF(resp, neededParamIndices);
                    // 返回参数后，重新设置开始时间
                    iTTableArray[workerId].startTime = System.currentTimeMillis();
                    iTTableArray[workerId].iteration = getMaxIteration(iTTableArray) + 1;

                    // 返回完参数之后开始更新时间，如果不在上述要求线程（等待线程）里面，那么需要进行策略选择，
                    // 如果在的话，上面已经选择过等待还是继续了，当可以继续执行时需要更新当前的一写ITTable的信息


                }


            } else {
                // 等待master的通知（在等待之后发送消息给master）
                FutureTask<Boolean> task = new FutureTask<>(() -> {
                    try {
                        WSP_WaitBarrier[workerId].set(true);
                        WSP_WaitBarrier[workerId].wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, Boolean.TRUE);
                synchronized (WSP_WaitBarrier[workerId]) {
                    new Thread(task).start();
                }

                // 保证了一定进入了上面对barrierForOtherServer[workerId]的锁

                while (!WSP_WaitBarrier[workerId].get()) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                synchronized (WSP_WaitBarrier[workerId]) {
                    WSP_WaitBarrier[workerId].set(false);
                }
                // 这样可以保证只有在上面锁释放的时候，才能通知master，该进程在等待
                Context.psRouterClient.getPsWorkers().get(Context.masterId).getBlockingStub().notifyNonMasterIsWaitingWSP(ServerIdAndWorkerId.newBuilder()
                        .setWorkerId(workerId)
                        .setServerId(ServerContext.serverId)
                        .build());
                // 等待master的notify
                while (!task.isDone()) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                RespTool.respParam_LMF(resp, neededParamIndices);
            }



        } else {
            RespTool.respParam_LMF(resp, neededParamIndices);
        }


    }


    public static ConcurrentSet getIOfMinNegGain(IterationTimeTable[] iTTableArray, StrategyChoiceTable[] sCTArray, int workerId) {
        float minValue = Float.MAX_VALUE;
        int minI = 0;
        ConcurrentSet<Integer> set = new ConcurrentSet<Integer>();
        for (int i = 0; i < sCTArray.length; i++) {
            if (sCTArray[i].negGain < minValue&&!isFinishIter[i].get()) {
                minValue = sCTArray[i].negGain;
                minI = i;
            }
        }
        if(minI!=workerId){
            for (int i = 0; i < iTTableArray.length; i++) {
                long minITime=iTTableArray[minI].startTime+iTTableArray[minI].execTime;
                long iTime=iTTableArray[i].startTime+iTTableArray[i].execTime;
                if (iTime <= minITime && i != workerId && curIterationOfWorker[i].get() < Context.trainRoundNum.get()) {
                    set.add(i);
                }
            }
        }


        return set;
    }

    public static int getMaxIteration(IterationTimeTable[] iTTableArray) {
        int max = 0;
        for (IterationTimeTable iTTable : iTTableArray) {
            if (iTTable.iteration > max) {
                max = iTTable.iteration;
            }
        }
        return max;
    }


}