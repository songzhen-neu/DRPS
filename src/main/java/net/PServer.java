package net;


import Util.MessageDataTransUtil;
import com.google.common.collect.Maps;

import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;
import context.Context;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import org.jblas.FloatMatrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import store.KVStore;

import java.io.IOException;

import java.util.Map;
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


    public PServer(int port){
        this.server = ServerBuilder.forPort(port).addService(net.PSGrpc.bindService(this)).build();
    }

    public void start() throws  IOException{
        this.server.start();
        logger.info("PServer Start");

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                PServer.this.stop();
            }
        });
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
        long step=globalStep.get();
        long wStep=workerStep.incrementAndGet();


        floatMatrixMap.put(req.getKey(),afMatrix);

        store.sumAFMatrix(afMatrix);
        while(store.getL().get()<2){
            try{
                Thread.sleep(10);
            }catch (Exception e){
                e.printStackTrace();
            }
        }



        responseObject.onNext(MessageDataTransUtil.FloatMatrix_2_MatrixMessage(store.getSum().get("1")));
        responseObject.onCompleted();

    }

    @Override
    public void aFMatrixDimPartition(KeyValueListMessage req,StreamObserver<PartitionListMessage> responseObject){
        Map<Long,Integer> map=MessageDataTransUtil.KeyValueListMessage_2_Map(req);
        store.mergeDim(map);
        store.getL().incrementAndGet();

        while(store.getL().get()< Context.workerNum){
            try{
                Thread.sleep(10);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        responseObject.onNext(null);
        responseObject.onCompleted();






    }


}