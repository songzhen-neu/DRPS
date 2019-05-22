package net;


import Util.DataProcessUtil;
import Util.MessageDataTransUtil;
import Util.PartitionUtil;
import Util.SetUtil;
import com.google.common.collect.Maps;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.AtomicDoubleArray;
import com.google.protobuf.Internal;
import context.Context;
import context.ServerContext;
import context.WorkerContext;
import dataStructure.partition.Partition;
import dataStructure.partition.PartitionList;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.ConcurrentSet;
import lombok.Data;
import lombok.Synchronized;
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
    private static PartitionList partitionList;


    //太乱了，下面是barrier专用变量
    private static AtomicInteger barrier_waitThread = new AtomicInteger(0);
    private static AtomicBoolean barrier_isExecuteFlag = new AtomicBoolean(false);

    private static AtomicBoolean isExecuteFlag_otherLocal = new AtomicBoolean(false);
    private static AtomicBoolean isFinished_otherLocal = new AtomicBoolean(false);
    private static AtomicBoolean isWait_otherLocal = new AtomicBoolean(false);
    private static AtomicInteger workerStepForBarrier_otherLocal = new AtomicInteger(0);
    private static AtomicInteger[][] afMatrix;

    private List<Set>[] ls_partitionedVSet = ServerContext.kvStoreForLevelDB.ls_partitionedVSet;

    private static AtomicDoubleArray diskAccessForV;

    private static AtomicDouble[][] commCost_temp;
    private static AtomicDouble[][] commCost;

    /** 用来存储每个server存储了哪些维度，i是sever，set是参数划分块*/
    private static ConcurrentSet[] paramAssignSetArray;

    /** 用来存储每个server都拥有哪些参数维度*/
    private static ConcurrentSet[] vSet;




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
    public void getIndexOfSparseDim(SListMessage req, StreamObserver<SLKVListMessage> responsedObject) {
        synchronized (ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim()) {
            try {
                Map<String, Long> map = ServerContext.kvStoreForLevelDB.getIndex(req);
                map.put("CurIndexNum", ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().longValue());
//            logger.info("curIndex:"+ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().longValue());

                SLKVListMessage slkvListMessage = MessageDataTransUtil.Map_2_SLKVListMessage(map);
//                logger.info(ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().toString());

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
    public void sendSparseDimSizeAndInitParams(InitVMessage req, StreamObserver<BMessage> responseObject) {
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
    public void getNeededParams(PullRequestMessage req, StreamObserver<SFKVListMessage> resp) {
        // 获取需要访问的参数的key
        Set<String> neededParamIndices = MessageDataTransUtil.ProtoStringList_2_Set(req.getNeededGradDimList());
        int workerId = req.getWorkerId();
        int iterationOfWi = req.getIteration();
        SFKVListMessage sfkvListMessage;
        try {
            switch (Context.parallelismControlModel) {
                case BSP:
                    waitBarrier();
                    sfkvListMessage = ServerContext.kvStoreForLevelDB.getNeededParams(neededParamIndices);
                    resp.onNext(sfkvListMessage);
                    resp.onCompleted();
                    break;
                case AP:
                    if(req.getWorkerId()!=ServerContext.serverId){
                        networkCount.set(networkCount.intValue()+neededParamIndices.size());
                    }
                    sfkvListMessage = ServerContext.kvStoreForLevelDB.getNeededParams(neededParamIndices);
                    resp.onNext(sfkvListMessage);
                    resp.onCompleted();
                    break;
                case SSP:
                    SSP.init();
                    SSP.isRespOrWaited(workerId, resp, neededParamIndices, iterationOfWi);
                    break;
                case SSP_S:
                    SSP.init();
                    SSP.isRespOrWaited(workerId, resp, neededParamIndices, iterationOfWi);
                    break;
                case WSP:
                    // Worker-Selection Parallelism Control Model
                    WSP.init();
                    WSP.isRespOrWaited(workerId, resp, neededParamIndices, iterationOfWi);
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
    public void sendGradMapLMF(SRListMessage req, StreamObserver<SMessage> resp) {
        Map<String, Float[]> map = MessageDataTransUtil.SRListMessage_2_Map(req);
        SMessage.Builder smessage = SMessage.newBuilder();

        ServerContext.kvStoreForLevelDB.updateParamsLMF(map);
        smessage.setStr("success");
        // 同步异步不要写在push操作里，要写在pull操作里
//        waitBarrier();

        resp.onNext(smessage.build());
        resp.onCompleted();
    }

    @Override
    @Synchronized
    public void sendCurIndexNum(LMessage req, StreamObserver<SMessage> resp) {
        ServerContext.kvStoreForLevelDB.setCurIndexOfSparseDim(new AtomicLong(req.getL()));
        SMessage.Builder sMessage = SMessage.newBuilder();
        sMessage.setStr("success");
        resp.onNext(sMessage.build());
        resp.onCompleted();
    }


    AtomicInteger barrier_sentInitedT = new AtomicInteger(0);

    @Override
    public void sendInitedT(IFMessage req, StreamObserver<IMessage> resp) {
        IMessage.Builder intMessage = IMessage.newBuilder();
        // 这里的意思显然是将
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
//            logger.info("IntBarrier:"+ IntBarrier);
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
            partitionList = PartitionUtil.initPartitionList(prunedVSet);
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
    @Synchronized
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
//    private static AtomicInteger minI = new AtomicInteger(-1);
//    private static AtomicDouble minValue = new AtomicDouble(Double.MAX_VALUE);

    @Override
    public void pushDiskAccessForV(InsertjIntoViMessage req, StreamObserver<FMessage> resp) {
        // DiskAccessForV
        float[] diskAccessForVFromWi = MessageDataTransUtil.FListMessage_2_FloatArray(req.getDiskTimeArray());

        diskAccessForV = new AtomicDoubleArray(diskAccessForVFromWi.length);
        workerStep_forPushDiskAccessForV.set(0);
        isExecuted_forPushDiskAccessForV.set(false);
        isFinished_forPushDiskAccessForV.set(false);
        int minI = -1;
        float minValue = Float.MAX_VALUE;

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

        if (!isExecuted_forPushDiskAccessForV.getAndSet(true)) {
            for (int i = 0; i < diskAccessForV.length(); i++) {
                if (diskAccessForV.get(i) < minValue) {
                    minValue = (float) diskAccessForV.get(i);
                    minI = i;
                }
            }
            // req.getInsertI()是insertId,如果创建了ls，则直接加入，否则重新创建一个，然后再加入
            // 这里的minI就是选择那种插入方案，就把该参数给哪一个set
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
        // 需要构建catToCatSetMap索引
//        Map<String,String> catToCatSetMap=new HashMap<String, String>();
//
//
//
//
//        ServerContext.kvStoreForLevelDB.catToCatSetMap=catToCatSetMap;
        resp.onNext(sMessage.build());
        resp.onCompleted();
    }

    @Override
    public void testGrpc(IMessage req, StreamObserver<IMessage> resp) {
        if (req.getI() == -1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        resp.onNext(IMessage.newBuilder().setI(req.getI()).build());
        resp.onCompleted();
        // onCompleted后的语句可以继续执行的
        System.out.println("haha1");
    }

    @Override
    public void notifyForSSP(IMessage req, StreamObserver<BMessage> resp) {
        /**
         *@Description: master发送给其他server，通知某些worker可以notify了
         *@Param: [req, resp]
         *@return: void
         *@Author: SongZhen
         *@date: 上午10:40 19-4-19
         */
        synchronized (SSP.barrierForOtherServer[req.getI()]) {
            SSP.barrierForOtherServer[req.getI()].notifyAll();
            resp.onNext(BMessage.newBuilder().setB(true).build());
            resp.onCompleted();
        }
    }


    @Override
    public void isWaiting(ServerIdAndWorkerId req, StreamObserver<BMessage> resp) {
        /**
         *@Description: 当除master之外的server都在等待时，通知master线程继续执行
         *@Param: [req, resp]
         *@return: void
         *@Author: SongZhen
         *@date: 下午3:55 19-4-18
         */

        // 这里代码写乱了，一共三台worker，req.getI，以及cyclivBarrier记录的都是worker的相关信息
        // 但是这里同步是需要server的信息，也就是其他两台server同时等待
        // 每个worker都会发（3个worker），每个worker发两次，要求每个worker发的这两次，有一次可以notify
        try {

            Context.cyclicBarrier_sub1[req.getWorkerId()].await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }


        while (Context.cyclicBarrier_sub1[req.getWorkerId()].getNumberWaiting() > 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        if (req.getServerId() == Context.masterId + 1) {
            Context.cyclicBarrier_sub1[req.getWorkerId()].reset();

            while (!SSP.isWaiting[req.getWorkerId()].get()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            synchronized (SSP.isWaiting[req.getWorkerId()]) {
                SSP.isWaiting[req.getWorkerId()].notifyAll();
            }
        }
        resp.onNext(BMessage.newBuilder().setB(true).build());
        resp.onCompleted();

    }

    @Override
    public void sendTrainRoundNum(IMessage req, StreamObserver<BMessage> resp) {
        Context.trainRoundNum.set(req.getI());
        resp.onNext(BMessage.newBuilder().setB(true).build());
        resp.onCompleted();
    }

    @Override
    public void notifyNonMasterIsWaitingWSP(ServerIdAndWorkerId req, StreamObserver<BMessage> resp) {
        try {
            Context.cyclicBarrier_sub1[req.getWorkerId()].await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }


        while (Context.cyclicBarrier_sub1[req.getWorkerId()].getNumberWaiting() > 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        if (req.getServerId() == Context.masterId + 1) {
            Context.cyclicBarrier_sub1[req.getWorkerId()].reset();

            while (!WSP.WSP_IsWaiting[req.getWorkerId()].get()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            synchronized (WSP.WSP_IsWaiting[req.getWorkerId()]) {
                WSP.WSP_IsWaiting[req.getWorkerId()].notifyAll();
            }
        }
        resp.onNext(BMessage.newBuilder().setB(true).build());
        resp.onCompleted();
    }

    @Override
    public void notifyForWSP(IMessage req, StreamObserver<BMessage> resp) {
        synchronized (WSP.WSP_WaitBarrier[req.getI()]) {
            WSP.WSP_WaitBarrier[req.getI()].notifyAll();
            resp.onNext(BMessage.newBuilder().setB(true).build());
            resp.onCompleted();
        }
    }

    @Override
    public void getBestPartition(IMessage req, StreamObserver<PartitionListMessage> resp) {
        PartitionListMessage partitionListMessage = MessageDataTransUtil.PartitionList_2_PartitionListMessage(partitionList);
        resp.onNext(partitionListMessage);
        resp.onCompleted();
    }


    CyclicBarrier cyclicBarrier_workerNum = new CyclicBarrier(Context.workerNum);
    AtomicBoolean isSatisfyMinGain = new AtomicBoolean(false);
    AtomicDouble[]  diskCost;

    @Override
    public void sendAFMatrix(AFMatrixMessage req, StreamObserver<BMessage> resp) {
        // 三个worker发送到该master中，需要将worker累加到一个统一的AFMatrix中
        // 首先，每个worker需要先将message转化成int[][]
        int[][] afMatrix_i = MessageDataTransUtil.AFMatrixMessage_2_AFMatrix(req);
        // afMatrix是Atomic类型，线程见可见
        // 需要有一个线程初始化afMatrix
        if (req.getReqHost() == Context.masterId) {
            afMatrix = new AtomicInteger[req.getRowCount()][req.getRowCount()];
            for (int i = 0; i < afMatrix.length; i++) {
                for (int j = 0; j < afMatrix[i].length; j++) {
                    afMatrix[i][j] = new AtomicInteger(0);
                }
            }
        }

        // 同步一下，确保所有线程都初始化了
        barrier_WorkerNum();

        // 开始进行累加
        for (int i = 0; i < afMatrix_i.length; i++) {
            for (int j = 0; j < afMatrix_i[i].length; j++) {
                afMatrix[i][j].addAndGet(afMatrix_i[i][j]);
            }
        }


        // 同步一下，确保都累加完毕
        barrier_WorkerNum();

        // 然后需要有一个worker对这些数据进行加和，然后计算出最优的组合策略，更新partitionList
        // 其他worker只需要进行等待，直到这个worker计算完，再返回
        if (req.getReqHost() == Context.masterId) { //这里采用的是master机器对应的worker进行计算
            // 需要定义代价数组，即两两组合的建立索引的时间代价
            int partitionListSize = partitionList.partitionList.size();
            float[][] costTime = new float[afMatrix.length][afMatrix.length];

            // 开始计算costTime
            for (int i = 0; i < partitionListSize; i++) {
                for (int j = 0; j < partitionListSize; j++) {
                    if (i == j) {
                        //前面的参数是磁盘访问的两个时间（seek和read），后面是partition[i]包含的Dim个数
                        costTime[i][i] = afMatrix[i][i].get() * cost(partitionList.partitionList.get(i).partition.size());
                    } else {
                        int mergePartitionSize = partitionList.partitionList.get(i).partition.size() + partitionList.partitionList.get(j).partition.size();
                        costTime[i][j] = (afMatrix[i][i].get() + afMatrix[j][j].get() - afMatrix[i][j].get()) * cost(mergePartitionSize);
                    }
                }

            }

            float maxTimeReduce = 0;
            int pi = 0;
            int pj = 0;

            // 计算最大的时间成本Reduce，也就是最佳合并pi，pj
            for (int i = 0; i < partitionListSize - 1; i++) {
                for (int j = i + 1; j < partitionListSize; j++) {
                    float costReduce = costTime[i][i] + costTime[j][j] - costTime[i][j];
                    // 这块要保证根据disk IO代价划分的划分块最大大小不能超过maxDiskPartitionNum，不然有些划分块太大，没办法做网络通信优化了
                    if (costReduce > maxTimeReduce
                            &&partitionList.partitionList.get(i).partition.size()<=Context.maxDiskPartitionNum
                            &&partitionList.partitionList.get(j).partition.size()<=Context.maxDiskPartitionNum) {
                        maxTimeReduce = costReduce;
                        pi = i;
                        pj = j;
                    }


                }
            }

            // 重新构建partitionList，也就是合并之后的partitionList
            // 如果小于最低收益的话，则不更新
            if (maxTimeReduce > Context.minGain) {
                System.out.println(pi + "," + pj);
                int pjSize = getPiSize(partitionList.partitionList, pj);
                for (int i = 0; i < pjSize; i++) {
                    partitionList.partitionList.get(pi).partition.add(partitionList.partitionList.get(pj).partition.get(i));
                }
                partitionList.partitionList.remove(pj);

            } else {
                isSatisfyMinGain.set(true);
                // 在划分结束后，且实现最优划分时，需要记录最佳划分中，每个划分块的访问时间
                // 这里让master机器的worker线程进行计算
                // 其实访问时间就是对角线的cost时间
                if(req.getReqHost()==Context.masterId){
                    diskCost=new AtomicDouble[costTime.length];
                    for(int i=0;i<diskCost.length;i++){
                        diskCost[i]=new AtomicDouble(costTime[i][i]);
                    }
                }

                // 这里需要把只有一个参数的划分块删除（不能删除，因为后面还有对网络通信的优化，这些访问频率高的对网络通信影响也大）
//                PartitionList partitionList_temp=new PartitionList();
//                for(int i=0;i<partitionList.partitionList.size();i++){
//                    Partition partition_temp=new Partition();
//                    if(partitionList.partitionList.get(i).partition.size()>1){
//                        for(int j=0;j<partitionList.partitionList.get(i).partition.size();j++){
//                            partition_temp.partition.add(partitionList.partitionList.get(i).partition.get(j));
//                        }
//                        partitionList_temp.partitionList.add(partition_temp);
//                    }
//                }
//                partitionList=partitionList_temp;



                // 这里还需要构建一个划分的反向索引，也就是可以通过参数找到其所在的划分
            }
        }

        // 同步一下，直到master线程执行完partitionList的更新后返回结果
        barrier_WorkerNum();
        resp.onNext(BMessage.newBuilder().setB(isSatisfyMinGain.get()).build());
        resp.onCompleted();

    }

    /*获取划分i的大小*/
    public static int getPiSize(List<Partition> partitionList, int pi) {
        return partitionList.get(pi).partition.size();
    }


    public static float cost(int singlePartitionSize) {
        /**
         *@Description: 这是计算代价损失，表示，如果划分中有一个元素，那么就是按照ParaKV的结构存的，
         * 如果大于1就是按照ParaKVPartition存的。一个ParaKV需要的字节数是70
         * 而一个ParaKVPartition的基础空间是200，每增加一个元素是增加18
         *@Param: [singlePartitionSize]
         *@return: float
         *@Author: SongZhen
         *@date: 上午8:24 18-11-16
         */
        if (singlePartitionSize == 1) {
            return (Context.diskSeekTime + (Context.setParamBaseSize_bytes+Context.singleParamOfSetSize_bytes) * Context.diskAccessTime);
        } else {
            return (Context.diskSeekTime + (singlePartitionSize * Context.singleParamOfSetSize_bytes + Context.setParamBaseSize_bytes) * Context.diskAccessTime);
        }

    }



    public void barrier_WorkerNum() {
        /**
         *@Description: 用来进行线程同步，同时在执行完reset了，可保证其复用性
         * 但是注意，这里只针对同步线程个数是workerNum的
         *@Param: []
         *@return: void
         *@Author: SongZhen
         *@date: 下午3:14 19-5-6
         */
        try {
            cyclicBarrier_workerNum.await();
            // 只让一个线程执行reset
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

    }



    @Override
    public void sendCommCost(CommCostMessage req,StreamObserver<VSetMessage> resp){
        // 现在收到了每个worker发送来的CommCost
        // 需要将CommCostMessage转化成float数组，也就是CommCost_i
        float[] commCost_i=MessageDataTransUtil.CommCostMessage_2_CommCost(req);

        if(req.getReqHost()==Context.masterId){
            // 因为要用到diskCost，但是如果不做磁盘优化，这个数据结构是null，这里需要初始化一下
            if(!Context.isOptimizeDisk){
                diskCost= new AtomicDouble[commCost_i.length];
                for(int i=0;i<diskCost.length;i++){
                    diskCost[i]=new AtomicDouble(0);
                }
            }


            // 第一维表示第i台机器对每个划分块的访问次数，第二维表示划分块的个数
            commCost_temp=new AtomicDouble[Context.workerNum][commCost_i.length];

            for(int i=0;i<commCost_temp.length;i++){
                for(int j=0;j<commCost_temp[i].length;j++){
                    commCost_temp[i][j]=new AtomicDouble(0);
                }
            }
            // 真正的通信代价，第一维表示每个划分块，第二维表示划分块放到这个server中的时间代价
            commCost=new AtomicDouble[commCost_i.length][Context.serverNum];

            for(int i=0;i<commCost.length;i++){
                for(int j=0;j<commCost[i].length;j++){
                    commCost[i][j]=new AtomicDouble(0);
                }
            }

        }

        barrier_WorkerNum();

        // 现在需要将各自的commCost_i整合到一个全局的float[][]里
        for(int i=0;i<commCost_i.length;i++){
            commCost_temp[req.getReqHost()][i].set(commCost_i[i]);
        }

        barrier_WorkerNum();

        // 开始用commCost计算每个划分块放到每个服务器上的时间代价comm
        if(req.getReqHost()==Context.masterId){
            for(int i=0;i<commCost.length;i++){
                for(int j=0;j<commCost[i].length;j++){
                    for(int k=0;k<Context.serverNum;k++){
                        if(j!=k){
                            // 也就是第i个划分放在第j台机器上的访问时间为，除了第j台机器外的其他机器k，对划分i的访问
                            commCost[i][j].set(commCost[i][j].get()+commCost_temp[k][i].get());
                        }
                    }
                    commCost[i][j].set(commCost[i][j].get()*Context.netTrafficTime);
                }
            }


            // 需要对磁盘时间和通信时间进行累加
            for(int i=0;i<commCost.length;i++){
                for(int j=0;j<commCost[i].length;j++){
                    // commCost[i][j]是第i个划分在第j台机器上的通信代价
                    // diskCost[i]是第i个划分的磁盘代价
                    commCost[i][j].set(commCost[i][j].get()+diskCost[i].get());
                }
            }

            // 磁盘和通信整体时间已经累加完成，现在需要进行划分了
            // 这里采用贪心策略进行划分,注意commCost的i表示第i个划分，j表示第j台机器

            // 先初始化paramAssignSetArray
            paramAssignSetArray=new ConcurrentSet[Context.serverNum];
            for(int i=0;i<paramAssignSetArray.length;i++){
                paramAssignSetArray[i]=new ConcurrentSet();
            }

            // 定义每个桶的当前代价,java通过new方法创建了会初始化为0.0
            float[] serverTotalCost=new float[Context.serverNum];
            // 定义set存储当前哪些参数没有被划分,这里是用从0,1,2,3,...的方式存的
            Set<Integer> notAssignedParamSet=new HashSet<Integer>();
            for(int i=0;i<partitionList.partitionList.size();i++){
                notAssignedParamSet.add(i);
            }


            // 开始进行贪心策略划分
            while(notAssignedParamSet.size()>0){
                // 先选择要插入的桶
                int minI_bucket=-1;
                float minValue_bucket=Float.MAX_VALUE;
                for(int i=0;i<serverTotalCost.length;i++){
                    if(serverTotalCost[i]<minValue_bucket){
                        minI_bucket=i;
                        minValue_bucket=serverTotalCost[i];
                    }
                }

                // 在桶min_i中再选择插入的节点
                int minI_node=-1;
                float minValue_node=Float.MAX_VALUE;
                for(int i=0;i<commCost.length;i++){
                    // 如果是miniI_bucket桶里值最小的，而且没有被分配
                    if(commCost[i][minI_bucket].get()<minValue_node&&notAssignedParamSet.contains(i)){
                        minI_node=i;
                        minValue_node=(float) commCost[i][minI_bucket].get();
                    }
                }

                // 分配miniI_node这个节点到miniI_bucket桶里，并且从notAssignedParamSet中删除minI_node
                // 注意paramAssignSetArray中i表示桶，set表示节点
                paramAssignSetArray[minI_bucket].add(minI_node);
                serverTotalCost[minI_bucket]=serverTotalCost[minI_bucket]+(float) commCost[minI_node][minI_bucket].get();
                notAssignedParamSet.remove(minI_node);

            }

            // 贪心策略完成后，得到paramAssignSetArray
            // 利用paramAssignSetArray和partitionList算出来vSet和ls_partitionedVSet
            // 先初始化vSet
            vSet=new ConcurrentSet[Context.serverNum];
            for(int i=0;i<vSet.length;i++){
                vSet[i]=new ConcurrentSet();
            }

            // 构建vSet
            // i表示每台server
            for(int i=0;i<paramAssignSetArray.length;i++){
                // j表示server中的划分块
                for(int j:(Set<Integer>)paramAssignSetArray[i]){
                    Partition partition=partitionList.partitionList.get(j);
                    for(long param:partition.partition){
                        vSet[i].add(param);
                    }
                }
            }

            // 构建ls_partitionedVSet，其实它是一个List<Set>[]的形式
            // i表示第i个server，list表示这个server中的划分块，Set表示每个划分的内容
            // 这里应该用partitionList加paramAssignSetArray构建
            // 第i个server
            for(int i=0;i<paramAssignSetArray.length;i++){
                // 第j个paramSet
                for(int j:(Set<Integer>)paramAssignSetArray[i]){
                    // 从partitionList中取出第j个paramSet
                    // 需要将原来的list类型转化成set类型，可以一个数一个数的加入到set中
                    Set<Long> set=SetUtil.List_2_Set(partitionList.partitionList.get(j).partition);
                    // 将该set加入到第i个机器的list中
                    ls_partitionedVSet[i].add(set);
                }
            }


        }


        barrier_WorkerNum();

        // 返回vSet，首先将vSet转化成vSetMessage
        VSetMessage vSetMessage=MessageDataTransUtil.VSet_2_VSetMessage(vSet);

        // 最后需要返回vset
        resp.onNext(vSetMessage);
        resp.onCompleted();

    }

    @Override
    public void setBestPartitionList(PartitionListMessage req,StreamObserver<BMessage> resp){
        partitionList=MessageDataTransUtil.PartitionListMessage_2_PartitionList(req);
        resp.onNext(BMessage.newBuilder().setB(true).build());
        resp.onCompleted();
    }

    @Override
    public void setLSPartitionVSet(LSetListArrayMessage req,StreamObserver<BMessage> resp){
        ls_partitionedVSet=MessageDataTransUtil.LSetListArrayMessage_2_SetListArray(req);
        ServerContext.kvStoreForLevelDB.ls_partitionedVSet=ls_partitionedVSet;
        resp.onNext(BMessage.newBuilder().setB(true).build());
        resp.onCompleted();
    }

    @Override
    public void sendIListMessage(IListMessage req,StreamObserver<BMessage> resp){
        /**
        *@Description: 这个函数用来测试网络通信和本地通信速度For TestClass/TestNetAndLocalTrafficSpeed.java
        *@Param: [req, resp]
        *@return: void
        *@Author: SongZhen
        *@date: 下午12:46 19-5-10
        */
        resp.onNext(BMessage.newBuilder().setB(true).build());
        resp.onCompleted();
    }

    @Override
    public void sendSparseDimSizeAndInitParamsLMF(InitVMessageLMF req, StreamObserver<BMessage> resp) {
        /**
        *@Description: 为低秩矩阵分解专门设计的初始化参数的函数
        *@Param: [req, resp]
        *@return: void
        *@Author: SongZhen
        *@date: 下午2:25 19-5-14
        */

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
            for (long l : req.getVSet(i).getLlistList()) {
                vSet[i].add(l);
            }
        }


        try {
            ServerContext.kvStoreForLevelDB.initParamsLMF(req.getUserNum(),req.getMovieNum(),req.getR(), vSet, ls_params);
            BMessage.Builder booleanMessage = BMessage.newBuilder();
            booleanMessage.setB(true);
            resp.onNext(booleanMessage.build());
            resp.onCompleted();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static AtomicInteger networkCount=new AtomicInteger(0);
    @Override
    public void getNeededParamsLMF(PullRequestMessage req, StreamObserver<SRListMessage> resp) {
        // 获取需要访问的参数的key
        Set<String> neededParamIndices = MessageDataTransUtil.ProtoStringList_2_Set(req.getNeededGradDimList());
        int workerId = req.getWorkerId();
        int iterationOfWi = req.getIteration();
        SRListMessage sMatrixListMessage;
        try {
            switch (Context.parallelismControlModel) {
                case BSP:
                    waitBarrier();
                    sMatrixListMessage = ServerContext.kvStoreForLevelDB.getNeededParams_LMF(neededParamIndices);
                    resp.onNext(sMatrixListMessage);
                    resp.onCompleted();
                    break;
                case AP:
                    if(req.getWorkerId()!=ServerContext.serverId){
                        networkCount.set(networkCount.intValue()+neededParamIndices.size());
                    }
                    sMatrixListMessage = ServerContext.kvStoreForLevelDB.getNeededParams_LMF(neededParamIndices);
                    resp.onNext(sMatrixListMessage);
                    resp.onCompleted();
                    break;
                case SSP:
                    SSP.init();
                    SSP.isRespOrWaited_LMF(workerId, resp, neededParamIndices, iterationOfWi);
                    break;
                case SSP_S:
                    SSP.init();
                    SSP.isRespOrWaited_LMF(workerId, resp, neededParamIndices, iterationOfWi);
                    break;
                case WSP:
                    // Worker-Selection Parallelism Control Model
                    WSP.init();
                    WSP.isRespOrWaited_LMF(workerId, resp, neededParamIndices, iterationOfWi);
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
    public void showSomeStatisticAfterTrain(BMessage req,StreamObserver<BMessage> resp){
        logger.info("磁盘IO次数："+ServerContext.kvStoreForLevelDB.diskIOCount);
        logger.info("通信个数："+networkCount);
        resp.onNext(BMessage.newBuilder().setB(true).build());
        resp.onCompleted();
    }


}