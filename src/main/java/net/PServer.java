package net;

import Util.MatrixUtil;
import com.google.common.collect.Maps;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import org.jblas.FloatMatrix;
import store.KVStore;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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

    private AtomicLong globalStep=new AtomicLong(0);
    private AtomicLong workerStep=new AtomicLong(0);

    public PServer(int port){
        this.server = ServerBuilder.forPort(port).addService(net.PSGrpc.bindService(this)).build();
    }

    public void start() throws  IOException{
        this.server.start();

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
        FloatMatrix afMatrix=MatrixUtil.MatrixMessage_2_FloatMatrix(req);
        long step=globalStep.get();
        long wStep=workerStep.incrementAndGet();
        while (wStep<step+2){
            try {
                Thread.sleep(100);
            }catch (Exception e){
                e.printStackTrace();
            }

            wStep=workerStep.get();
        }

        store.sumAFMatrix(afMatrix);


    }


}