package net;


import Util.MessageDataTransUtil;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jblas.FloatMatrix;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static Util.DataProcessUtil.isCatEmpty;
import static io.grpc.okhttp.internal.Platform.logger;

/**
 * @program: simplePsForModelPartition
 * @description: worker
 * @author: SongZhen
 * @create: 2018-12-02 19:21
 */
public class PSWorker {
    private ManagedChannel channel=null;
    private net.PSGrpc.PSBlockingStub blockingStub=null;

    public void setChannel(String host,int port){
        channel=ManagedChannelBuilder.forAddress(host,port).usePlaintext(true).build();
        blockingStub=net.PSGrpc.newBlockingStub(channel);
    }



    public void pushKeyValueMap() {
        Map<Long, Long> map = new HashMap<Long, Long>();
        map.put(1l, 5l);
        map.put(5l, 3l);
        map.put(15l,10l );

        KeyValueListMessage keyValueListMessage=MessageDataTransUtil.Map_2_KeyValueListMessage(map);
        PartitionListMessage partitionListMessage=blockingStub.aFMatrixDimPartition(keyValueListMessage);
        System.out.println("sss");

    }

    public void shutdown() throws InterruptedException{
        channel.shutdown();
    }

    public void getCat_indexFromServer(int catSize,String[] lineSplit,long[] cat){
        SListMessage.Builder sListMessage=SListMessage.newBuilder();
        sListMessage.setSize(catSize);
        Set<String> catIndexSet=new HashSet<String>();
        for (int i = 2; i < 2 + catSize; i++) {
            if (isCatEmpty(lineSplit[i])) {
                cat[i - 2] = -1;
            } else {
                sListMessage.addList(lineSplit[i]);
            }
        }

        SLKVListMessage slkvListMessage=blockingStub.getIndexOfSparseDim(sListMessage.build());
        Map<String,Long> map=MessageDataTransUtil.SLKVListMessage_2_map(slkvListMessage);

        for (int i = 2; i < 2 + catSize; i++) {
            if (!lineSplit[i].equals("")) {
                cat[i-2]=map.get(lineSplit[i]);
            }
        }

    }


    public Map<String,Long> getCatDimMapBySet(Set<String> catSet){
        Map<String,Long> catDimMap=new HashMap<String, Long>();
        SListMessage.Builder slistMessage=SListMessage.newBuilder();
        for(String cat:catSet){
            slistMessage.addList(cat);
        }
        slistMessage.setSize(catSet.size());
        SLKVListMessage slkvListMessage=blockingStub.getIndexOfSparseDim(slistMessage.build());

        for(int i=0;i<slkvListMessage.getSize();i++){
            catDimMap.put(slkvListMessage.getList(i).getKey(),slkvListMessage.getList(i).getValue());
        }
        return catDimMap;
    }

    public long getSparseDimSize()throws UnknownHostException {
        RequestMetaMessage.Builder requestMetaMessage=RequestMetaMessage.newBuilder();
        requestMetaMessage.setHost(Inet4Address.getLocalHost().getHostAddress());
        LongMessage longMessage=blockingStub.getSparseDimSize(requestMetaMessage.build());
        return longMessage.getL();

    }

    public void sentSparseDimSizeAndInitParams(long sparseDimSize){
        LongMessage.Builder l=LongMessage.newBuilder();
        l.setL(sparseDimSize);
        blockingStub.sentSparseDimSizeAndInitParams(l.build());
    }





}