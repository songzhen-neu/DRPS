package net;


import Util.MessageDataTransUtil;
import com.google.common.collect.Maps;

import com.google.common.util.concurrent.AtomicDoubleArray;
import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;
import context.Context;
import context.ServerContext;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import org.iq80.leveldb.DB;
import org.jblas.FloatMatrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import store.KVStore;

import java.io.IOException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private Executor updateThread= Executors.newSingleThreadExecutor();
    private Map<String,String> updateKeys= Maps.newConcurrentMap();
    private KVStore store=new KVStore();
    private Map<String,FloatMatrix> floatMatrixMap=new ConcurrentHashMap<String, FloatMatrix>();

    private AtomicLong globalStep=new AtomicLong(0);
    private AtomicLong workerStep=new AtomicLong(0);
    static Logger logger=LoggerFactory.getLogger((PServer.class));
    AtomicBoolean finished=new AtomicBoolean(false);
    private AtomicBoolean workerStepInited=new AtomicBoolean(false);
    private float[] maxFeature=new float[Context.featureSize];
    private float[] minFeature=new float[Context.featureSize];



    public PServer(int port){
        this.server = ServerBuilder.forPort(port).addService(net.PSGrpc.bindService(this)).build();
    }

    public void start() throws  IOException{
        this.server.start();
        logger.info("PServer Start");
        init();

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                PServer.this.stop();
            }
        });
    }


    public void init(){
        // 初始化feature的min和max数组
        for(int i=0;i<maxFeature.length;i++){
            maxFeature[i]=Float.MIN_VALUE;
            minFeature[i]=Float.MAX_VALUE;

        }
    }

    public void stop(){
        if(this.server!=null){
            server.shutdown();
        }
    }

    public void blockUntilShutdown()throws InterruptedException{
        if(server!=null){
            server.awaitTermination();
        }
    }

    @Override
    public void pushAFMatrix(MatrixMessage req,StreamObserver<MatrixMessage> responseObject){
        store.getL().set(0);
        FloatMatrix afMatrix=MessageDataTransUtil.MatrixMessage_2_FloatMatrix(req);


        floatMatrixMap.put(req.getKey(),afMatrix);

        store.sumAFMatrix(afMatrix);
        while(store.getL().get()<Context.workerNum){
            try{
                Thread.sleep(10);
            }catch (Exception e){
                e.printStackTrace();
            }
        }



        responseObject.onNext(MessageDataTransUtil.FloatMatrix_2_MatrixMessage(store.getSum().get("freq")));
        responseObject.onCompleted();

    }

    @Override
    public void aFMatrixDimPartition(KeyValueListMessage req,StreamObserver<PartitionListMessage> responseObject){
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
    public void getIndexOfSparseDim(SListMessage req,StreamObserver<SLKVListMessage> responsedObject){
        try{
            Map<String,Long> map=ServerContext.kvStoreForLevelDB.getIndex(req);
            SLKVListMessage slkvListMessage=MessageDataTransUtil.Map_2_SLKVListMessage(map);
            logger.info(ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().toString());
            responsedObject.onNext(slkvListMessage);
            responsedObject.onCompleted();
        }catch (IOException e){
            e.printStackTrace();
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }

    }

    @Override
    public void getSparseDimSize(RequestMetaMessage req,StreamObserver<LongMessage> reponseObject){
        LongMessage.Builder sparseDimSize=LongMessage.newBuilder();

        logger.info("host:"+req.getHost());
        workerStep.incrementAndGet();

        while(workerStep.longValue()<Context.workerNum){
            try {
                Thread.sleep(10);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }


        sparseDimSize.setL(ServerContext.kvStoreForLevelDB.getCurIndexOfSparseDim().longValue());
        reponseObject.onNext(sparseDimSize.build());
        reponseObject.onCompleted();
    }

    @Override
    public void sentSparseDimSizeAndInitParams(LongMessage req,StreamObserver<BooleanMessage> responseObject){
        Context.sparseDimSize=req.getL();
        // 开始利用sparseDimSize，采用取余的方式进行数据分配
        try{
            ServerContext.kvStoreForLevelDB.initParams();

            BooleanMessage.Builder booleanMessage=BooleanMessage.newBuilder();
            booleanMessage.setB(true);
            responseObject.onNext(booleanMessage.build());
            responseObject.onCompleted();


        }catch (IOException e){
            e.printStackTrace();
        }

    }


    @Override
    public void barrier(RequestMetaMessage req,StreamObserver<BooleanMessage> resp){
        waitBarrier();

        BooleanMessage.Builder boolMessage=BooleanMessage.newBuilder();
        boolMessage.setB(true);
        logger.info(""+workerStep.longValue());
        resp.onNext(boolMessage.build());
        resp.onCompleted();

    }

    @Override
    public void getMaxAndMinValueOfEachFeature(MaxAndMinArrayMessage req,StreamObserver<MaxAndMinArrayMessage> resp){
        float[] reqMax=new float[req.getMaxCount()];
        float[] reqMin=new float[req.getMinCount()];

        for(int i=0;i<reqMax.length;i++){
            reqMax[i]=req.getMax(i);
            reqMin[i]=req.getMin(i);
        }


        synchronized (this){
            for(int i=0;i<Context.featureSize;i++){
                if(reqMax[i]>maxFeature[i]){
                    maxFeature[i]=reqMax[i];
                }
                if(reqMin[i]<minFeature[i]){
                    minFeature[i]=reqMin[i];
                }
            }
        }

        waitBarrier();

        MaxAndMinArrayMessage.Builder respMaxAndMin=MaxAndMinArrayMessage.newBuilder();
        for(int i=0;i<Context.featureSize;i++){
            respMaxAndMin.addMax(maxFeature[i]);
            respMaxAndMin.addMin(minFeature[i]);
        }

        resp.onNext(respMaxAndMin.build());
        resp.onCompleted();



    }




    public void waitBarrier(){
        if(!workerStepInited.get()){
            workerStep.set(0);
            workerStepInited.getAndSet(true);
        }
        workerStep.incrementAndGet();
        while(workerStep.get()<Context.workerNum){
            try {
                Thread.sleep(10);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        workerStepInited.set(false);
    }

    @Override
    public void getNeededParams(SListMessage req,StreamObserver<SFKVListMessage> resp){
        // 获取需要访问的参数的key
        Set<String> neededParamIndices=MessageDataTransUtil.SListMessage_2_Set(req);
        try {
            SFKVListMessage sfkvListMessage=ServerContext.kvStoreForLevelDB.getNeededParams(neededParamIndices);
            resp.onNext(sfkvListMessage);
            resp.onCompleted();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

}