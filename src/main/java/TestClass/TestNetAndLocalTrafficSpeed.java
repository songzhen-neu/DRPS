package TestClass;

import Util.CurrentTimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import context.Context;
import context.WorkerContext;
import fi.iki.elonen.NanoHTTPD;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import net.IListMessage;
import net.PSWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import visual.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-10 12:40
 */
public class TestNetAndLocalTrafficSpeed {
    /**
    *@Description: 经过测试，在局域网大概100MB/S的传输速度的时候，传本地和传remote速度差不太多
     * 大概以下几组数据
     * remote：739
     * local：537
    *@Param:
    *@return:
    *@Author: SongZhen
    *@date: 下午1:49 19-5-10
    */
    public static void main(String[] args)throws IOException {
        Context.init();
        WorkerContext.init();
        int messageLength=20000000;
        IListMessage.Builder message= net.IListMessage.newBuilder();
        for(int i=0;i<messageLength;i++){
            message.addList(i);
        }


        PSWorker psWorker_remote=new PSWorker("192.168.111.102",9011);
        PSWorker psWorker_local=new PSWorker("localhost",9010);





        CurrentTimeUtil.setStartTime();
        psWorker_remote.getBlockingStub().sendIListMessage(message.build());
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("remote");

        CurrentTimeUtil.setStartTime();
        psWorker_local.getBlockingStub().sendIListMessage(message.build());
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("local");

        CurrentTimeUtil.setStartTime();
        psWorker_remote.getBlockingStub().sendIListMessage(message.build());
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("remote");

        CurrentTimeUtil.setStartTime();
        psWorker_local.getBlockingStub().sendIListMessage(message.build());
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("local");


    }

    /**
     * @program: simplePsForModelPartition
     * @description:
     * @author: SongZhen
     * @create: 2019-06-24 14:47
     */
    public static class UiServer_test extends NanoHTTPD implements UiServerGrpc.UiServer {
        static Logger logger = LoggerFactory.getLogger(UiServer_test.class);

        ObjectMapper objectMapper = new ObjectMapper();

        ConcurrentHashMap<String, List<Float>> xs = new ConcurrentHashMap<>();

        ConcurrentHashMap<String, List<Float>> ys = new ConcurrentHashMap<>();

        Server server;

        public UiServer_test() throws IOException {
            super(8888);
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            server = ServerBuilder.forPort(8990).addService(UiServerGrpc.bindService(this)).build();
            try {
                server.start();
                server.awaitTermination();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                close();
            }
        }

        public void close() {
            server.shutdown();
        }

        public static void main(String[] args) {
            try {
                new UiServer_test();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static boolean isFinished=false;
        @Override
        public Response serve(IHTTPSession session) {
            String msg = "";
            Map<String, Object> result = Maps.newHashMap();
            Map<String, String> parms = session.getParms();
            if("data".equals(parms.get("act"))){


                for(int i=0;i<4;i++){
                    List<Float> ls=new ArrayList<Float>();
                    for(int j=0;j<10;j++){
                        ls.add(new Random().nextFloat());
                    }
                    result.put("x"+i,ls);

                }

                try {
                    return newFixedLengthResponse(objectMapper.writeValueAsString(result));
                }catch (JsonProcessingException e){
                    e.printStackTrace();
                    return newFixedLengthResponse(e.getMessage());
                }

            }else {
                return newFixedLengthResponse(TestDataSet.readToString(UiServer.class.getResource("").getPath() + "../../../src/main/resources/web/Point_PlotTest.html"));
            }



        }


        @Override
        public void plot(PlotMessage req, StreamObserver<PlotMessage> resp) {
            List<Float> x=req.getData().getXList();
            List<Float> y=req.getData().getYList();
            resp.onNext(PlotMessage.newBuilder().build());
            resp.onCompleted();
        }

        @Override
        public void plotScatterGraph(plotScatterGraphMessage request, StreamObserver<Flag> responseObserver) {

        }

        @Override
        public void plotWorkerProcess(workerProcessMessage request, StreamObserver<Flag> responseObserver) {

        }
    }
}