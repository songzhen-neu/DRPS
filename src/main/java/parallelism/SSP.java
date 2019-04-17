package parallelism;

import context.Context;
import context.ServerContext;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.ConcurrentSet;
import net.SFKVListMessage;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-04-17 21:04
 */
public class SSP {
    public static ConcurrentSet[] barrier;
    public static AtomicInteger[] count;
    public static AtomicInteger[] iteration;
    public static AtomicBoolean isInited = new AtomicBoolean(false);
    public static final int bound=Context.boundForSSP;

    public static void init(){
        synchronized (isInited){
            if (!isInited.getAndSet(true)){
                barrier=new ConcurrentSet[Context.workerNum];
                count=new AtomicInteger[Context.workerNum];
                iteration=new AtomicInteger[Context.workerNum];

                for(int i=0;i<Context.workerNum;i++){
                    barrier[i]=new ConcurrentSet();
                    count[i]=new AtomicInteger(0);
                    iteration[i]=new AtomicInteger(0);
                }
            }
        }

    }

    public static void isRespOrWaited(int workerId, StreamObserver<SFKVListMessage> resp,Set<String> neededParamIndices){
        for(int j=0;j<barrier.length;j++){
            if(barrier[j].contains(workerId)){
                count[j].incrementAndGet();
                if(count[j].get()==barrier[j].size()){
                    synchronized (barrier[j]){
                        barrier[j].notify();
                    }
                }
            }
        }

        iteration[workerId].set(getMaxIteration(iteration)+1);

        // 把所有迭代次数小于iteration[workerId]-2的进程全部加入barrier里
        for(int i=0;i<iteration.length&&i!=workerId;i++){
            if(iteration[i].get()<=iteration[workerId].get()-bound){
                barrier[workerId].add(iteration[i].get());
            }
        }
        if(barrier[workerId].size()==0){
            // 直接返回结果
            respParam(resp, neededParamIndices);

        }else {
            synchronized (barrier[workerId]){
                try {
                    barrier[workerId].wait();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
        }

        respParam(resp, neededParamIndices);

        barrier[workerId].clear();
        count[workerId].set(0);

    }

    public static void respParam(StreamObserver<SFKVListMessage> resp, Set<String> neededParamIndices) {
        try {
            SFKVListMessage sfkvListMessage = ServerContext.kvStoreForLevelDB.getNeededParams(neededParamIndices);
            resp.onNext(sfkvListMessage);
            resp.onCompleted();
        }catch (ClassNotFoundException|IOException e){
            e.printStackTrace();
        }
    }

    public static int getMaxIteration(AtomicInteger[] iteration){
        int maxValue=Integer.MIN_VALUE;
        for(int i=0;i<iteration.length;i++){
            if(iteration[i].get()>maxValue){
                maxValue=iteration[i].get();
            }
        }
        return maxValue;
    }


}