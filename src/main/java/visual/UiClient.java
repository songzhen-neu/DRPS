package visual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;
import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-06-24 15:08
 */
public class UiClient {
    ManagedChannel channel;
    UiServerGrpc.UiServerFutureStub featureStub;
    private static UiClient client=new UiClient();

    public static UiClient ins(){
        return client;
    }

    public UiClient(){
        this("localhost",8990);
    }
    public UiClient(String host,int port){
        channel=ManagedChannelBuilder.forAddress(host,port).usePlaintext(true).build();
        featureStub=UiServerGrpc.newFutureStub(channel).withCompression("gzip");
    }

    public void close(){
        channel.shutdown();
    }

    public void plot(String id,float x,float y){
        featureStub.plot(PlotMessage.newBuilder().setId(id).setData(Plot.newBuilder().addX(x).addY(y).build()).build());
    }

    public void plotScatterGraph(String id, Map<String,List<Double>> map){
        plotScatterGraphMessage.Builder message=plotScatterGraphMessage.newBuilder();
        PlotDouble.Builder plotDouble1=PlotDouble.newBuilder();
        PlotDouble.Builder plotDouble2=PlotDouble.newBuilder();
        for(double d:map.get("l1x")){
            plotDouble1.addX(d);
        }
        for(double d:map.get("l1y")){
            plotDouble1.addY(d);
        }
        for(double d:map.get("l2x")){
            plotDouble2.addX(d);
        }
        for(double d:map.get("l2y")){
            plotDouble2.addY(d);
        }
        message.addPointCollection(plotDouble1);
        message.addPointCollection(plotDouble2);
        featureStub.plotScatterGraph(message.build());
    }
}