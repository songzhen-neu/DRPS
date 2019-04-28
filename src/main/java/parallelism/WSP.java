package parallelism;

import context.Context;
import context.ServerContext;
import dataStructure.parallelismControlModel.IterationTimeTable;
import dataStructure.parallelismControlModel.StrategyChoiceTable;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.ConcurrentSet;
import lombok.Data;
import lombok.Synchronized;
import net.SFKVListMessage;

import java.io.IOException;
import java.sql.Time;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    public static AtomicInteger[] curIterationOfWorker=new AtomicInteger[Context.workerNum];

    @Synchronized
    public static void init() {
        synchronized (isInited) {
            if (!isInited.getAndSet(true)) {
                // 没有初始化，那么执行以下代码
                for(int i=0;i<iTTableArray.length;i++){
                    iTTableArray[i]=new IterationTimeTable();
                }
                for(int i=0;i<sCTArray.length;i++){
                    sCTArray[i]=new StrategyChoiceTable();
                }
                for(int i=0;i<optimalPlanSet.length;i++){
                    optimalPlanSet[i]=new ConcurrentSet();
                }

                for (int i = 0; i < Context.workerNum; i++) {
                    count[i]=new AtomicInteger(0);
                    iTTableArray[i].startTime = System.currentTimeMillis();
                    iTTableArray[i].iteration = 0;
                    barrier_forWSP[i]=new AtomicBoolean(false);
                    curIterationOfWorker[i]=new AtomicInteger(0);
                }

            }
        }
    }

    public static AtomicBoolean isFinished = new AtomicBoolean(false);
    public static AtomicInteger barrier_forIsRespOrWaited = new AtomicInteger(0);
    public static AtomicBoolean isContainedInOtherPlan = new AtomicBoolean(false);
    public static AtomicBoolean isFirstItaration = new AtomicBoolean(true);
    public static CyclicBarrier cyclicBarrier = new CyclicBarrier(Context.workerNum);


    public static void isRespOrWaited(int workerId, StreamObserver<SFKVListMessage> resp, Set<String> neededParamIndices, int iterationOfWi) throws ClassNotFoundException, IOException, InterruptedException {
        if(Context.workerNum>1){
            if(ServerContext.serverId==Context.masterId){
                for (int i = 0; i < Context.workerNum; i++) {
                    if (optimalPlanSet[i].contains(workerId)) {
                        synchronized (barrier_forWSP[i]) {
                            count[i].incrementAndGet();
                            if (optimalPlanSet[i].size() == count[i].get()) {
                                barrier_forWSP[i].notifyAll();
                                optimalPlanSet[i].clear();
                            } else {
//                        count[i].incrementAndGet();
                                barrier_forWSP[i].wait();
                            }
                        }



                        // 返回完参数之后开始更新时间，如果不在上述要求线程（等待线程）里面，那么需要进行策略选择，
                        // 如果在的话，上面已经选择过等待还是继续了，当可以继续执行时需要更新当前的一写ITTable的信息

                        // 只执行一次这段代码
                        synchronized (isFinished) {
                            if (isFinished.getAndSet(true)) {
                                iTTableArray[i].execTime = System.currentTimeMillis() - iTTableArray[i].startTime;
                                iTTableArray[i].startTime = System.currentTimeMillis();
                                iTTableArray[i].endTime = iTTableArray[i].startTime + iTTableArray[i].endTime;
                                int iteration = getMaxIteration(iTTableArray);
                                iTTableArray[i].iteration = iteration + 1;

                            }
                        }

                        isContainedInOtherPlan.set(true);
                    }
                }


                // 如果不包含在其他worker的执行方案里，那么需要进行策略选择
                if (!isContainedInOtherPlan.get()) {
                    if (isFirstItaration.get()) {
                        iTTableArray[workerId].endTime = System.currentTimeMillis();
                        iTTableArray[workerId].execTime = iTTableArray[workerId].endTime - iTTableArray[workerId].startTime;
                        // 同步代码
                        try {
                            cyclicBarrier.await();
                        }catch (BrokenBarrierException e){
                            e.printStackTrace();
                        }

                        while(cyclicBarrier.getNumberWaiting()>0){
                            Thread.sleep(10);
                        }
                        cyclicBarrier.reset();
                        isFirstItaration.set(false);
                        RespTool.respParam(resp,neededParamIndices);

                        iTTableArray[workerId].startTime=System.currentTimeMillis();
                        iTTableArray[workerId].endTime=iTTableArray[workerId].startTime+iTTableArray[workerId].execTime;
                        iTTableArray[workerId].iteration=1;
                    }else {
                        int maxIteration=getMaxIteration(iTTableArray);
                        iTTableArray[workerId].endTime=System.currentTimeMillis();
                        for(int i=0;i<Context.workerNum;i++){
                            sCTArray[i].waitTime=iTTableArray[workerId].endTime-iTTableArray[i].iteration;
                            if(i!=workerId){
                                sCTArray[i].staleness=maxIteration+1-iTTableArray[i].iteration;
                            }else {
                                sCTArray[i].staleness=0;
                            }
                            sCTArray[i].negGain=sCTArray[i].waitTime+sCTArray[i].staleness;

                        }
                        System.out.println("waitTime1111111111111111111111111:"+sCTArray[workerId].waitTime);
                        System.out.println("staleness1111111111111111111111111:"+sCTArray[workerId].staleness);
                        optimalPlanSet[workerId]=getIOfMinNegGain(iTTableArray,sCTArray,workerId);
                        if(optimalPlanSet[workerId].size()==0){
                            RespTool.respParam(resp,neededParamIndices);
                        }else {
                            synchronized (barrier_forWSP[workerId]){
                                barrier_forWSP[workerId].wait();
                            }
                            RespTool.respParam(resp,neededParamIndices);
                        }

                    }
                }else {
                    RespTool.respParam(resp,neededParamIndices);
                }
                synchronized (isFinished){
                    isFinished.set(false);
                }
            }else {

            }

        }else {
            RespTool.respParam(resp,neededParamIndices);
        }


    }


    public static ConcurrentSet getIOfMinNegGain(IterationTimeTable[] iTTableArray,StrategyChoiceTable[] sCTArray,int workerId){
        float minValue=Float.MAX_VALUE;
        int minI=0;
        ConcurrentSet<Integer> set=new ConcurrentSet<Integer>();
        for(int i=0;i<sCTArray.length;i++){
            if(sCTArray[i].negGain<minValue){
                minValue=sCTArray[i].negGain;
                minI=i;
            }
        }
        for(int i=0;i<iTTableArray.length;i++){
            if(iTTableArray[i].endTime<=iTTableArray[minI].endTime&&i!=workerId&&curIterationOfWorker[i].get()<Context.trainRoundNum.get()){
                set.add(i);
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