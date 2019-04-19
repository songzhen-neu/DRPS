package net;


import Util.DataProcessUtil;
import Util.MemoryUtil;
import Util.MessageDataTransUtil;
import com.google.common.collect.Maps;

import com.google.common.util.concurrent.AtomicDoubleArray;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import dataStructure.enumType.ParallelismControlModel;
import dataStructure.parallelismControlModel.IterationTimeTable;
import dataStructure.parallelismControlModel.StrategyChoiceTable;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.ConcurrentSet;
import lombok.Data;
import lombok.Synchronized;
import org.iq80.leveldb.DB;
import org.jblas.FloatMatrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parallelism.SSP;
import parallelism.WSP;
import store.KVStore;

import java.io.IOException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;


/**
 * @program: simplePsForModelPartition
 * @description: 参数服务器server端
 * @author: SongZhen
 * @create: 2018-12-02 17:59
 */

@Data
public class PServer implements net.PSGrpc.PS {
    private Server server;
    private Executor updateThread = Executors.newSingleThreadExecutor();
    private Map<String, String> updateKeys = Maps.newConcurrentMap();
    private KVStore store = new KVStore();
    private Map<String, FloatMatrix> floatMatrixMap = new ConcurrentHashMap<String, FloatMatrix>();

    private AtomicInteger globalStep = new AtomicInteger(0);
    private AtomicInteger workerStep = new AtomicInteger(0);
    static Logger logger = LoggerFactory.getLogger((PServer.class));
    AtomicBoolean finished = new AtomicBoolean(false);
    private AtomicBoolean workerStepInited = new AtomicBoolean(false);
    private float[] maxFeature = new float[Context.featureSize];
    private float[] minFeature = new float[Context.featureSize];

    private static AtomicBoolean isExecuteFlag = new AtomicBoolean(false);
    private static AtomicInteger workerStepForBarrier = new AtomicInteger(0);

    private ConcurrentSet<Float> numSet_otherWorkerAccessVi = new ConcurrentSet<Float>();
    private static volatile Float floatSum = 0f;
    private static AtomicBoolean isFinished = new AtomicBoolean(false);
    private static AtomicBoolean isStart = new AtomicBoolean(false);


    private static ConcurrentMap<Long, Integer> vAccessNumMap = new ConcurrentHashMap<Long, Integer>();
    private static ConcurrentSet<Long> prunedVSet = new ConcurrentSet<Long>();
    private static AtomicInteger waitThread = new AtomicInteger(0);


    //太乱了，下面是barrier专用变量
    private static AtomicInteger barrier_waitThread = new AtomicInteger(0);
    private static AtomicBoolean barrier_isExecuteFlag = new AtomicBoolean(false);

    private static AtomicBoolean isExecuteFlag_otherLocal = new AtomicBoolean(false);
    private static AtomicBoolean isFinished_otherLocal = new AtomicBoolean(false);
    private static AtomicBoolean isWait_otherLocal = new AtomicBoolean(false);
    private static AtomicInteger workerStepForBarrier_otherLocal = new AtomicInteger(0);


    private List<Set>[] ls_partitionedVSet = ServerContext.kvStoreForLevelDB.ls_partitionedVSet;

    private static AtomicDoubleArray diskAccessForV;




//    CyclicBarrier barrier = new CyclicBarrier(Context.workerNum);
//    CyclicBarrier barrier_2 = new CyclicBarrier(Context.workerNum - 1);

    public PServer(int port) {
        this.server = NettyServerBuilder.forPort(port).maxMessageSize(Context.maxMessageSize).addService(net.PSGrpc.bindService(this)).build();
    }

    public void start() throws IOException {
        this.server.start();
        logger.info("PServer Start");
        init();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                PServer.this.stop();
            }
        });
    }


    public void init() {
        // 初始化feature的min和max数组
        for (int i = 0; i < maxFeature.length; i++) {
            maxFeature[i] = Float.MIN_VALUE;
            minFeature[i] = Float.MAX_VALUE;

        }


        for (int i = 0; i < Context.serverNum; i++) {
            ls_partitionedVSet[i] = new ArrayList<Set>();
        }

    }

    public void stop() {
        if (this.server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    @Override
    public void pushAFMatrix(MatrixMessage req, StreamObserver<MatrixMessage> responseObject) {
        store.getL().set(0);
        FloatMatrix afMatrix = MessageDataTransUtil.MatrixMessage_2_FloatMatrix(req);


        floatMatrixMap.put(req.getKey(), afMatrix);

        store.sumAFMatrix(afMatrix);
        while (store.getL().get() < Context.workerNum) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        responseObject.onNext(MessageDataTransUtil.FloatMatrix_2_MatrixMessage(store.getSum().get("freq")));
        responseObject.onCompleted();

    }

    @Override
    public void aFMatrixDimPartition(KeyValueListMessage req, StreamObserver<PartitionListMessage> responseObject) {
//        Map<Long,Integer> map=MessageDataTransUtil.KeyValueListMessage_2_Map(req);
//        store.mergeDim(map);
//        store.getL().incrementAndGet();
//
//        while(store.getL().get()< Context.workerNum){
//            try{
//                Thread.sleep(10);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//
//        responseObject.onNext(null);
//        responseObject.onCompleted();
        return;

    }

    @Override
    public void getIndexOfSparseDim(SListMessage req, StreamObserver<SLKVListMessage> responsedObject) {
        synchronized (ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim()) {
            try {
                Map<String, Long> map = ServerContext.kvStoreForLevelDB.getIndex(req);
                map.put("CurIndexNum", ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().longValue());
//            logger.info("curIndex:"+ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().longValue());

                SLKVListMessage slkvListMessage = MessageDataTransUtil.Map_2_SLKVListMessage(map);
                logger.info(ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().toString());

                responsedObject.onNext(slkvListMessage);
                responsedObject.onCompleted();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void getSparseDimSize(RequestMetaMessage req, StreamObserver<LMessage> reponseObject) {
        LMessage.Builder sparseDimSize = LMessage.newBuilder();

        logger.info("host:" + req.getHost());
        workerStep.incrementAndGet();

        while (workerStep.longValue() < Context.workerNum) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        sparseDimSize.setL(ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().longValue());
        reponseObject.onNext(sparseDimSize.build());
        reponseObject.onCompleted();
    }

    @Override
    public void sentSparseDimSizeAndInitParams(InitVMessage req, StreamObserver<BMessage> responseObject) {
        /**
         *@Description: 初始化各自server服务器的参数到leveldb中
         *@Param: [req, responseObject]
         *@return: void
         *@Author: SongZhen
         *@date: 下午2:37 19-3-10
         */
        Context.sparseDimSize = req.getL();
        // 这里面就包含了这个server要存的所有参数
        List<Set> ls_params = ls_partitionedVSet[ServerContext.serverId];
        // 把ls_partitionVset中元素数量是1的删除




        // 显示ls_partitionVSet
        DataProcessUtil.printLs_partitionedVset(ls_partitionedVSet);

        // 开始利用sparseDimSize，采用取余的方式进行数据分配
        // 把LList转换成Set
        Set[] vSet = new Set[Context.serverNum];
        for (int i = 0; i < vSet.length; i++) {
            vSet[i] = new HashSet<Long>();
        }

        // 把list转换成Set[]
        for (int i = 0; i < vSet.length; i++) {
            for (long l : req.getList(i).getLlistList()) {
                vSet[i].add(l);
            }
        }


        try {
            ServerContext.kvStoreForLevelDB.initParams(req.getL(), vSet, ls_params);
            BMessage.Builder booleanMessage = BMessage.newBuilder();
            booleanMessage.setB(true);
            responseObject.onNext(booleanMessage.build());
            responseObject.onCompleted();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    AtomicInteger barrier = new AtomicInteger(0);

    @Override
    public void barrier(RequestMetaMessage req, StreamObserver<BMessage> resp) {

        synchronized (barrier) {
            barrier.incrementAndGet();
            if (barrier.get() == Context.workerNum) {
                barrier.notifyAll();
            } else {
                try {
                    barrier.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

        synchronized (barrier) {
            barrier.set(0);
        }

        BMessage.Builder boolMessage = BMessage.newBuilder();
        boolMessage.setB(true);
//        logger.info(""+workerStep.longValue());
        resp.onNext(boolMessage.build());
        resp.onCompleted();

    }

    @Override
    public void getMaxAndMinValueOfEachFeature(MaxAndMinArrayMessage req, StreamObserver<MaxAndMinArrayMessage> resp) {
        float[] reqMax = new float[req.getMaxCount()];
        float[] reqMin = new float[req.getMinCount()];

        for (int i = 0; i < reqMax.length; i++) {
            reqMax[i] = req.getMax(i);
            reqMin[i] = req.getMin(i);
        }


        synchronized (this) {
            for (int i = 0; i < Context.featureSize; i++) {
                if (reqMax[i] > maxFeature[i]) {
                    maxFeature[i] = reqMax[i];
                }
                if (reqMin[i] < minFeature[i]) {
                    minFeature[i] = reqMin[i];
                }
            }
        }


        waitBarrier();

        MaxAndMinArrayMessage.Builder respMaxAndMin = MaxAndMinArrayMessage.newBuilder();
        for (int i = 0; i < Context.featureSize; i++) {
            respMaxAndMin.addMax(maxFeature[i]);
            respMaxAndMin.addMin(minFeature[i]);
        }

        resp.onNext(respMaxAndMin.build());
        resp.onCompleted();


    }


    public void waitBarrier() {
        try {
            if (!barrier_isExecuteFlag.getAndSet(true)) {

                while (barrier_waitThread.get() < (Context.workerNum - 1)) {
                    Thread.sleep(10);
                }

                synchronized (barrier_waitThread) {


                    barrier_waitThread.set(0);
                    barrier_waitThread.notifyAll();
                }

            } else {
                synchronized (barrier_waitThread) {

                    barrier_waitThread.incrementAndGet();
                    barrier_waitThread.wait();
                }

            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        barrier_isExecuteFlag.set(false);


    }




    @Override
    public void getNeededParams(SSListMessage req, StreamObserver<SFKVListMessage> resp) {
        // 获取需要访问的参数的key
        Set<String> neededParamIndices = MessageDataTransUtil.SSListMessage_2_Set(req);
        int workerId=req.getWorkerId();
        SFKVListMessage sfkvListMessage;
        try {
            switch (Context.parallelismControlModel){
                case BSP:
                    waitBarrier();
                    sfkvListMessage = ServerContext.kvStoreForLevelDB.getNeededParams(neededParamIndices);
                    resp.onNext(sfkvListMessage);
                    resp.onCompleted();
                    break;
                case AP:
                    sfkvListMessage = ServerContext.kvStoreForLevelDB.getNeededParams(neededParamIndices);
                    resp.onNext(sfkvListMessage);
                    resp.onCompleted();
                    break;
                case SSP:
                    SSP.init();
                    SSP.isRespOrWaited(workerId,resp,neededParamIndices);

                    break;
                case WSP:
                    // Worker-Selection Parallelism Control Model
                    WSP.init();
                    WSP.isRespOrWaited(workerId,resp,neededParamIndices);
//                    WSP.chooseNextStre


                    break;
                default:
                    System.out.println("run to default");
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void sendSFMap(SFKVListMessage req, StreamObserver<SMessage> resp) {
        Map<String, Float> map = MessageDataTransUtil.SFKVListMessage_2_Map(req);
        SMessage.Builder smessage = SMessage.newBuilder();

        ServerContext.kvStoreForLevelDB.updateParams(map);
        smessage.setStr("success");
        // 同步异步不要写在push操作里，要写在pull操作里
//        waitBarrier();

        resp.onNext(smessage.build());
        resp.onCompleted();
    }

    @Override
    @Synchronized
    public void sentCurIndexNum(LMessage req, StreamObserver<SMessage> resp) {
        ServerContext.kvStoreForLevelDB.setCurIndexOfSparseDim(new AtomicLong(req.getL()));
        SMessage.Builder sMessage = SMessage.newBuilder();
        sMessage.setStr("success");
        resp.onNext(sMessage.build());
        resp.onCompleted();
    }


    AtomicInteger barrier_sentInitedT = new AtomicInteger(0);

    @Override
    public void sentInitedT(IFMessage req, StreamObserver<IMessage> resp) {
        IMessage.Builder intMessage = IMessage.newBuilder();

        ServerContext.kvStoreForLevelDB.getTimeCostMap().put(req.getI(), req.getF());
//        logger.info("TimeCostMapSize:"+ServerContext.kvStoreForLevelDB.getTimeCostMap().size());
        try {
            synchronized (ServerContext.kvStoreForLevelDB.getTimeCostMap()) {
                if (ServerContext.kvStoreForLevelDB.getTimeCostMap().size() == Context.workerNum) {
                    ServerContext.kvStoreForLevelDB.getTimeCostMap().notifyAll();

                } else {
                    ServerContext.kvStoreForLevelDB.getTimeCostMap().wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        isFinished.set(false);
        waitThread.set(0);
        isExecuteFlag.set(false);
        waitBarrier();
//        logger.info("isFinish1:"+isFinished+",:waitThread:"+waitThread);
        if (!isExecuteFlag.getAndSet(true)) {
            ServerContext.kvStoreForLevelDB.getMinTimeCostI().set(getKeyOfMinValue());
            while (waitThread.get() < (Context.workerNum - 1)) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            isFinished.set(true);
//            logger.info("test1");

        }
//        logger.info("test2");
        waitFinishedWithWaitThread();


//        logger.info("I:" + req.getI() + ",F:" + req.getF() + ",minI:" + ServerContext.kvStoreForLevelDB.getMinTimeCostI().get());


        intMessage.setI(ServerContext.kvStoreForLevelDB.getMinTimeCostI().get());


        synchronized (barrier_sentInitedT) {
            barrier_sentInitedT.incrementAndGet();
            if (barrier_sentInitedT.intValue() == Context.serverNum) {
                barrier_sentInitedT.notifyAll();
            } else {
                try {
                    barrier_sentInitedT.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        synchronized (barrier_sentInitedT) {
            barrier_sentInitedT.set(0);
        }


        ServerContext.kvStoreForLevelDB.getMinTimeCostI().set(0);
        synchronized (ServerContext.kvStoreForLevelDB.getTimeCostMap()) {
            ServerContext.kvStoreForLevelDB.getTimeCostMap().clear();
        }

        resp.onNext(intMessage.build());
        resp.onCompleted();

    }

    private void waitFinishedWithWaitThread() {
        try {
            if (isFinished.get()) {

                synchronized (isFinished) {
                    isFinished.notifyAll();

                }
            } else {
                synchronized (isFinished) {
                    waitThread.incrementAndGet();
                    isFinished.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void waitFinished() {
        try {
            if (isFinished.get()) {
                synchronized (isFinished) {
                    isFinished.notifyAll();
                }
            } else {
                synchronized (isFinished) {

                    isFinished.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isExecuteFlag.set(false);
        isFinished.set(false);
        waitThread.set(0);
    }


    public int getKeyOfMinValue() {
        int keyOfMaxValue = -1;
        Map<Integer, Float> map = ServerContext.kvStoreForLevelDB.getTimeCostMap();
        float minValue = Float.MAX_VALUE;
        for (int i : map.keySet()) {
//            System.out.println("i:"+i);
            if (keyOfMaxValue == -1) {
                keyOfMaxValue = i;
                minValue = map.get(i);
            } else {
                if (map.get(i) < minValue) {
                    keyOfMaxValue = i;
                    minValue = map.get(i);
                }
            }
        }
        return keyOfMaxValue;
    }

    AtomicInteger IntBarrier = new AtomicInteger(0);

    @Override
    public void pushLocalViAccessNum(FMessage req, StreamObserver<BMessage> resp) {

        numSet_otherWorkerAccessVi.add(req.getF());

//        logger.info("1");
        synchronized (IntBarrier) {
            IntBarrier.incrementAndGet();
            logger.info("IntBarrier:"+ IntBarrier);
            if (IntBarrier.intValue() >= Context.workerNum - 1) {
                IntBarrier.notifyAll();
                IntBarrier.set(0);
            } else {
                try {
                    IntBarrier.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
//        logger.info("2");

        // 开始计算numSet_otherWorkerAccessVi的总和,只允许计算一遍（一个线程计算）
        synchronized (floatSum) {
//            logger.info("isExecuteFlag:"+isExecuteFlag_otherLocal.get());
                if (!isExecuteFlag_otherLocal.getAndSet(true)) {
                    // 如果pull线程没有等待，则阻塞
                    while (!isWait_otherLocal.get()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }


                    for (float f : numSet_otherWorkerAccessVi) {
                        floatSum += f;
                    }

//                logger.info("iswait:"+isWait_otherLocal.get());

                    synchronized (isFinished_otherLocal) {
                        isFinished_otherLocal.notifyAll();
                    }


                }




        }

//        logger.info("3");
        // 同步
        try {
            synchronized (workerStepForBarrier_otherLocal) {
                workerStepForBarrier_otherLocal.incrementAndGet();
                if (workerStepForBarrier_otherLocal.get() == Context.workerNum - 1) {
                    workerStepForBarrier_otherLocal.notifyAll();
                } else {
                    workerStepForBarrier_otherLocal.wait();
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        logger.info("4");
//        System.out.println("num2:"+num_waitOthers);
//        System.out.println(workerStepForBarrier.intValue());
        synchronized (workerStepForBarrier_otherLocal) {
            workerStepForBarrier_otherLocal.set(0);
        }
        synchronized (isExecuteFlag_otherLocal) {
            isExecuteFlag_otherLocal.set(false);
        }

//        synchronized (IntBarrier) {
//            IntBarrier.set(0);
//        }

//        logger.info("5");
        BMessage.Builder executeStatus = BMessage.newBuilder();
        executeStatus.setB(true);

        resp.onNext(executeStatus.build());
        resp.onCompleted();


    }

    @Override
    public void pullOtherWorkerAccessForVi(RequestMetaMessage req, StreamObserver<FMessage> resp) {

        synchronized (isFinished_otherLocal) {
            try {
                isWait_otherLocal.set(true);
                // 这里是只有是多台机器的时候才wait，单机跑不wait
                if (Context.workerNum > 1) {
                    isFinished_otherLocal.wait();
                }


            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        synchronized (isWait_otherLocal) {
            isWait_otherLocal.set(false);
        }


        FMessage.Builder floatMessage = FMessage.newBuilder();
        floatMessage.setF(floatSum);
        floatSum = 0f;
        resp.onNext(floatMessage.build());
        resp.onCompleted();

    }

    @Override
    public void pushVANumAndGetCatPrunedRecord(LIListMessage req, StreamObserver<LListMessage> resp) {
        Map<Long, Integer> map = MessageDataTransUtil.LIListMessage_2_Map(req);


        // 下面开始计算，且只计算一次各个map的和
        synchronized (vAccessNumMap) {
            for (long l : map.keySet()) {
                if (vAccessNumMap.containsKey(l)) {
                    int num = vAccessNumMap.get(l) + map.get(l);

                    vAccessNumMap.put(l, num);
                } else {
                    vAccessNumMap.put(l, map.get(l));
                }
            }
        }

        waitBarrier();

        // 取频率高于freqThreshold,统计到
        if (!isExecuteFlag.getAndSet(true)) {
            for (Long l : vAccessNumMap.keySet()) {
                if (vAccessNumMap.get(l) > Context.freqThreshold) {
                    prunedVSet.add(l);
                }
            }
//            System.out.println("isFinish02:"+isFinished.get());
            isFinished.set(true);
//            System.out.println("isFinish03:"+isFinished.get());
        }

//        System.out.println("isFinish04:"+isFinished.get());

        waitFinished();

        isFinished.set(false);
        isExecuteFlag.set(false);


//        System.out.println("isFinish05:");
        LListMessage.Builder respMessage = LListMessage.newBuilder();
        for (long l : prunedVSet) {
            respMessage.addL(l);
        }
//        System.out.println("isFinish06:");
//        logger.info("prunedVSet"+prunedVSet.size());
        resp.onNext(respMessage.build());
        resp.onCompleted();


    }

    public void waitBarrier2(int num_waitOthers) {
        try {
            workerStepForBarrier.incrementAndGet();
            if (workerStepForBarrier.get() == num_waitOthers) {
                synchronized (workerStepForBarrier) {
                    workerStepForBarrier.notifyAll();
                }

            } else {
                synchronized (workerStepForBarrier) {
                    workerStepForBarrier.wait();
                }

            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        System.out.println("num2:"+num_waitOthers);
//        System.out.println(workerStepForBarrier.intValue());
        workerStepForBarrier.set(0);


    }

    @Override
    public void pullPartitionedVset(IMessage req, StreamObserver<ListSetMessage> resp) {
        /**
         *@Description: 主要是worker用来获取参数在各个server中怎么划分的（提高disk IO）,返回的仅仅是insertI对应的划分，而不是所有划分，所以就是个List<Set>
         *@Param: [req, resp]
         *@return: void
         *@Author: SongZhen
         *@date: 下午2:06 19-1-24
         */
        ListSetMessage lsMessage = MessageDataTransUtil.ListSet_2_ListSetMessage(ls_partitionedVSet[req.getI()]);
        resp.onNext(lsMessage);
        resp.onCompleted();

    }


    private static AtomicInteger insertI = new AtomicInteger(-1);

    @Override
    public void addInitedPartitionedVSet(LIMessage req, StreamObserver<BMessage> resp) {
        // insertId
        insertI.set(req.getI());
        Set<Long> set = new LinkedHashSet<Long>();
        // j_last
        set.add(req.getL());
        ls_partitionedVSet[req.getI()].add(set);
        BMessage.Builder bMessage = BMessage.newBuilder();
        bMessage.setB(true);
        resp.onNext(bMessage.build());
        resp.onCompleted();
    }


    private static AtomicInteger workerStep_forPushDiskAccessForV = new AtomicInteger(0);
    private static AtomicBoolean isExecuted_forPushDiskAccessForV = new AtomicBoolean(false);
    private static AtomicBoolean isFinished_forPushDiskAccessForV = new AtomicBoolean(false);
    private static AtomicInteger barrie_forPushDiskAccessForV = new AtomicInteger(0);

    @Override
    public void pushDiskAccessForV(InsertjIntoViMessage req, StreamObserver<FMessage> resp) {
        // DiskAccessForV
        float[] diskAccessForVFromWi = MessageDataTransUtil.FListMessage_2_FloatArray(req.getDiskTimeArray());

        diskAccessForV = new AtomicDoubleArray(diskAccessForVFromWi.length);
        workerStep_forPushDiskAccessForV.set(0);
        isExecuted_forPushDiskAccessForV.set(false);
        isFinished_forPushDiskAccessForV.set(false);

        synchronized (barrie_forPushDiskAccessForV) {
            barrie_forPushDiskAccessForV.incrementAndGet();
            if (barrie_forPushDiskAccessForV.intValue() == Context.workerNum) {
                barrie_forPushDiskAccessForV.notifyAll();
            } else {
                try {
                    barrie_forPushDiskAccessForV.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }


        synchronized (barrie_forPushDiskAccessForV) {
            barrie_forPushDiskAccessForV.set(0);
        }


        synchronized (diskAccessForV) {
            for (int i = 0; i < diskAccessForV.length(); i++) {
                diskAccessForV.addAndGet(i, diskAccessForVFromWi[i]);
            }
        }

        workerStep_forPushDiskAccessForV.incrementAndGet();

        while (workerStep_forPushDiskAccessForV.get() < Context.workerNum) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        // 下面开始选一个最小的作为插入的partition
        int minI = -1;
        float minValue = Float.MAX_VALUE;
        if (!isExecuted_forPushDiskAccessForV.getAndSet(true)) {
            for (int i = 0; i < diskAccessForV.length(); i++) {
                if (diskAccessForV.get(i) < minValue) {
                    minValue = (float) diskAccessForV.get(i);
                    minI = i;
                }
            }
            // req.getInsertI()是insertId,如果创建了ls，则直接加入，否则重新创建一个，然后再加入
            if (minI < ls_partitionedVSet[req.getInsertI()].size()) {
                // req.getJ()要插入的Long
                ls_partitionedVSet[req.getInsertI()].get(minI).add(req.getJ());
            } else {
                ls_partitionedVSet[req.getInsertI()].add(new HashSet());
                ls_partitionedVSet[req.getInsertI()].get(minI).add(req.getJ());
            }


            isFinished_forPushDiskAccessForV.set(true);
        }
        while (!isFinished_forPushDiskAccessForV.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 目前还没有按照ls_partitionedVSet进行参数划分

        FMessage.Builder fMessage = FMessage.newBuilder();
        fMessage.setF(minValue);
        resp.onNext(fMessage.build());
        resp.onCompleted();
    }

    @Override
    public void getLsPartitionedVSet(SMessage req, StreamObserver<LSetListArrayMessage> resp) {
        LSetListArrayMessage lSetListArrayMessage = MessageDataTransUtil.SetListArray_2_LSetListArrayMessage(ls_partitionedVSet);
        resp.onNext(lSetListArrayMessage);
        resp.onCompleted();
    }

    @Override
    public void putLsPartitionedVSet(LSetListArrayMessage req, StreamObserver<SMessage> resp) {
        SMessage.Builder sMessage = SMessage.newBuilder();
        sMessage.setStr("" + ServerContext.serverId);
        ServerContext.kvStoreForLevelDB.ls_partitionedVSet = MessageDataTransUtil.LSetListArrayMessage_2_SetListArray(req);
        ls_partitionedVSet = ServerContext.kvStoreForLevelDB.ls_partitionedVSet;
        resp.onNext(sMessage.build());
        resp.onCompleted();
    }

    @Override
    public void testGrpc(IMessage req,StreamObserver<IMessage> resp){
        if(req.getI()==-1){
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }

        }
        resp.onNext(IMessage.newBuilder().setI(req.getI()).build());
        resp.onCompleted();
        // onCompleted后的语句可以继续执行的
        System.out.println("haha1");
    }

    @Override
    public void notifyForSSP(IMessage req,StreamObserver<BMessage> resp){
        /**
        *@Description: master发送给其他server，通知某些worker可以notify了
        *@Param: [req, resp]
        *@return: void
        *@Author: SongZhen
        *@date: 上午10:40 19-4-19
        */
        synchronized (SSP.barrier[req.getI()]){
            SSP.barrier[req.getI()].notifyAll();
            resp.onNext(BMessage.newBuilder().setB(true).build());
            resp.onCompleted();
        }
    }


    @Override
    public void isWaiting(IMessage req,StreamObserver<BMessage> resp){
        /**
        *@Description: 当除master之外的server都在等待时，通知master线程继续执行
        *@Param: [req, resp]
        *@return: void
        *@Author: SongZhen
        *@date: 下午3:55 19-4-18
        */
        try{
            System.out.println("111");
            Context.cyclicBarrier_sub1[req.getI()].await();
        }catch (BrokenBarrierException|InterruptedException e){
            e.printStackTrace();
        }

        System.out.println("222");
        while (Context.cyclicBarrier_sub1[req.getI()].getNumberWaiting()>0){
            try {
                Thread.sleep(1);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        Context.cyclicBarrier_sub1[req.getI()].reset();
        System.out.println("333");
        if(req.getI()==Context.masterId+1){
            System.out.println("666");
            while(!SSP.isWaiting[req.getI()].get()){
                try{
                    Thread.sleep(10);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            System.out.println("555");
            synchronized (SSP.isWaiting[req.getI()]){
                System.out.println("haha1");
                SSP.isWaiting[req.getI()].notifyAll();
            }
        }
        System.out.println("444");
    }
}