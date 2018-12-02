package net;

import com.google.common.collect.Maps;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Data;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @program: simplePsForModelPartition
 * @description: 参数服务器server端
 * @author: SongZhen
 * @create: 2018-12-02 17:59
 */

@Data
public class PServer implements net.PSGrpc.PS {
    private Server server;
    private int workNum;
    private Executor updateThread= Executors.newSingleThreadExecutor();
    private Map<String,String> updateKeys= Maps.newConcurrentMap();

    public PServer(int port, int workNum){
        this.server = ServerBuilder.forPort(port).addService(net.PSGrpc.bindService(this)).build();
        this.workNum=workNum;
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
    public void sayHello(HelloRequest req,StreamObserver<HelloReply> responseObserver){
        HelloReply reply=HelloReply.newBuilder().setMessage("Hello"+req.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }


}