package net;

import Util.MatrixUtil;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jblas.FloatMatrix;

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


    public void push(){
        FloatMatrix floatMatrix=new FloatMatrix();
        float[] data=new float[25];
        for(int i=0;i<25;i++){
            data[i]=1;
        }
        floatMatrix.data=data;
        floatMatrix.columns=5;
        floatMatrix.rows=5;



        MatrixMessage matrixMessage = blockingStub.pushAFMatrix(MatrixUtil.FloatMatrix_2_MatrixMessage(floatMatrix));




    }

    public void shutdown() throws InterruptedException{
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }


}