package net;

import Gradient.GradientStructure;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

/**
 * @program: simplePsForModelPartition
 * @description: worker
 * @author: SongZhen
 * @create: 2018-12-02 19:21
 */
public class PSWorker {
    private final ManagedChannel channel;
    private final net.PSGrpc.PSBlockingStub blockingStub;

    public  PSWorker(String host,int port){
        channel=ManagedChannelBuilder.forAddress(host,port).usePlaintext(true).build();
        blockingStub=net.PSGrpc.newBlockingStub(channel);

    }

    public void greet(String name){
        HelloRequest request=HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        response=blockingStub.sayHello(request);

    }

    public void getGradient(String name){
        HelloRequest request=HelloRequest.newBuilder().setName(name).build();
        Gradient response;
        response=blockingStub.getGradient(request);
    }

    public void shutdown() throws InterruptedException{
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }


}