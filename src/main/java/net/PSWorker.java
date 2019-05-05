package net;


import Util.MessageDataTransUtil;


import context.Context;
import context.WorkerContext;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import lombok.Data;
import lombok.Synchronized;
import org.jblas.FloatMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static Util.DataProcessUtil.isCatEmpty;


/**
 * @program: simplePsForModelPartition
 * @description: worker
 * @author: SongZhen
 * @create: 2018-12-02 19:21
 */

@Data
public class PSWorker {
    private ManagedChannel channel = null;
    private net.PSGrpc.PSBlockingStub blockingStub = null;
    private net.PSGrpc.PSFutureStub futureStub = null;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public PSWorker(String host, int port) {
        channel = NettyChannelBuilder.forAddress(host, port).maxMessageSize(Context.maxMessageSize).usePlaintext(true).build();
        blockingStub = net.PSGrpc.newBlockingStub(channel);
        futureStub = net.PSGrpc.newFutureStub(channel);
    }


    public void pushKeyValueMap() {
        Map<Long, Long> map = new HashMap<Long, Long>();
        map.put(1l, 5l);
        map.put(5l, 3l);
        map.put(15l, 10l);

        KeyValueListMessage keyValueListMessage = MessageDataTransUtil.Map_2_KeyValueListMessage(map);
        PartitionListMessage partitionListMessage = blockingStub.aFMatrixDimPartition(keyValueListMessage);
        System.out.println("sss");

    }


    public void shutdown() throws InterruptedException {
        channel.shutdown();
    }

    public void getCat_indexFromServer(int catSize, String[] lineSplit, long[] cat) {
        SListMessage.Builder sListMessage = SListMessage.newBuilder();
        sListMessage.setSize(catSize);
        Set<String> catIndexSet = new HashSet<String>();
        for (int i = 2; i < 2 + catSize; i++) {
            if (isCatEmpty(lineSplit[i])) {
                cat[i - 2] = -1;
            } else {
                sListMessage.addList(lineSplit[i]);
            }
        }

        SLKVListMessage slkvListMessage = blockingStub.getIndexOfSparseDim(sListMessage.build());
        Map<String, Long> map = MessageDataTransUtil.SLKVListMessage_2_map(slkvListMessage);

        for (int i = 2; i < 2 + catSize; i++) {
            if (!lineSplit[i].equals("")) {
                cat[i - 2] = map.get(lineSplit[i]);
            }
        }

    }

    @Synchronized
    public Map<String, Long> getCatDimMapBySet(Set<String> catSet) {
        Map<String, Long> catDimMap = new HashMap<String, Long>();
        SListMessage.Builder slistMessage = SListMessage.newBuilder();
        for (String cat : catSet) {
            slistMessage.addList(cat);
        }
        slistMessage.setSize(catSet.size());
        SLKVListMessage slkvListMessage = blockingStub.getIndexOfSparseDim(slistMessage.build());

        for (int i = 0; i < slkvListMessage.getSize(); i++) {
            catDimMap.put(slkvListMessage.getList(i).getKey(), slkvListMessage.getList(i).getValue());
        }
        return catDimMap;
    }

    public long getSparseDimSize() throws UnknownHostException {
        RequestMetaMessage.Builder requestMetaMessage = RequestMetaMessage.newBuilder();
        requestMetaMessage.setHost(Inet4Address.getLocalHost().getHostAddress());
        LMessage longMessage = blockingStub.getSparseDimSize(requestMetaMessage.build());
        return longMessage.getL();

    }

    public void sentSparseDimSizeAndInitParams(long sparseDimSize, Set<Long>[] vSet) {
        InitVMessage.Builder message = InitVMessage.newBuilder();
        message.setL(sparseDimSize);
        for (int i = 0; i < vSet.length; i++) {
            ILListKVMessage.Builder kvMessage = ILListKVMessage.newBuilder();
            kvMessage.setKey(i);
            for (Long l : vSet[i]) {
                kvMessage.addLlist(l);
            }
            message.addList(kvMessage.build());
        }


        blockingStub.sentSparseDimSizeAndInitParams(message.build());
    }

    public void barrier() throws UnknownHostException {
        RequestMetaMessage.Builder req = RequestMetaMessage.newBuilder();
        req.setHost(Inet4Address.getLocalHost().getHostAddress());
        blockingStub.barrier(req.build());
    }

    public void getGlobalMaxMinOfFeature(float[] max, float[] min) {
        /**
         *@Description: 直接对max和min进行修改，max和min传入的是本地最大最小值，返回的是全局最大最小值
         *@Param: [max, min]
         *@return: void
         *@Author: SongZhen
         *@date: 下午4:24 18-12-19
         */
        MaxAndMinArrayMessage.Builder req = MaxAndMinArrayMessage.newBuilder();

        // 不太确定repeated是不是有序的
        for (int i = 0; i < max.length; i++) {
            req.addMax(max[i]);
            req.addMin(min[i]);
        }

        MaxAndMinArrayMessage resp = blockingStub.getMaxAndMinValueOfEachFeature(req.build());
        for (int i = 0; i < Context.featureSize; i++) {
            max[i] = req.getMax(i);
            min[i] = req.getMin(i);
        }
    }


    public Future<SFKVListMessage> getNeededParams(Set<String> set,int workerId,int curIteration) {
        PullRequestMessage pullRequestMessage = MessageDataTransUtil.Set_2_PullRequestMessage(set,workerId,curIteration);
        Future<SFKVListMessage> sfkvListMessage = futureStub.getNeededParams(pullRequestMessage);
        return sfkvListMessage;

    }

    public void sendGradientMap(Map<String, Float> map) {
        SFKVListMessage sentMessage = MessageDataTransUtil.Map_2_SFKVListMessage(map);
        SMessage sMessage = blockingStub.sendSFMap(sentMessage);

    }

    @Synchronized
    public synchronized void setCurIndexNum(long curIndexNum) {
        LMessage.Builder l = LMessage.newBuilder();
        l.setL(curIndexNum);
        blockingStub.sentCurIndexNum(l.build());

    }

    public int sentInitedT(float Time,int insertI) {
        /**
         *@Description: 根据Time获取当前应该插入的server的id
         *@Param: [Time]
         *@return: int
         *@Author: SongZhen
         *@date: 上午8:49 19-1-24
         */
        int minCostI;
        IFMessage.Builder sentMessage = IFMessage.newBuilder();
        sentMessage.setF(Time);
        sentMessage.setI(insertI);
        IMessage respM = blockingStub.sentInitedT(sentMessage.build());
        minCostI = respM.getI();
//        logger.info("get Min time along machines:"+minCostI);
        return minCostI;
    }

    public void pushLocalViAccessNum(float T_localAccessVj) {
        /**
         *@Description: 如果不是要插入Vj的机器i，那么就把自己本地对Vj的访问次数push给server
         * 然后server统计完一个和之后返回给是插入Vj的机器i。
         *@Param: [T_localAccessVj]
         *@return: void
         *@Author: SongZhen
         *@date: 下午10:29 19-1-19
         */
        FMessage.Builder sentMessage = FMessage.newBuilder();
        sentMessage.setF(T_localAccessVj);
        blockingStub.pushLocalViAccessNum(sentMessage.build());

    }

    public float pullOtherWorkerAccessForVi() throws UnknownHostException {
        /**
         *@Description: 插入vj的机器i，向worker发送pull，请求其他机器访问Vj的次数
         *@Param: []
         *@return: void
         *@Author: SongZhen
         *@date: 下午10:31 19-1-19
         */
        RequestMetaMessage.Builder req = RequestMetaMessage.newBuilder();
        req.setHost(Inet4Address.getLocalHost().getHostName());
        FMessage accessedNum_otherWorkers = blockingStub.pullOtherWorkerAccessForVi(req.build());
        return accessedNum_otherWorkers.getF();
    }

    public Set<Long> pushVANumAndGetCatPrunedRecord(Map<Long, Integer> vAccessNum) {
        LIListMessage message = MessageDataTransUtil.Map_2_LIListMessage(vAccessNum);
        LListMessage lListMessage = blockingStub.pushVANumAndGetCatPrunedRecord(message);

        return MessageDataTransUtil.LListMessage_2_Set(lListMessage);
    }


    public List<Set> pullPartitionedVset(int insertId) {
        IMessage.Builder req = IMessage.newBuilder();

        req.setI(insertId);


        ListSetMessage lsMessage = blockingStub.pullPartitionedVset(req.build());
        return MessageDataTransUtil.ListSetMessage_2_ListSet(lsMessage);
    }

    public void addInitedPartitionedVSet(Long j, int insertId) {
        LIMessage.Builder req = LIMessage.newBuilder();
        req.setL(j);
        req.setI(insertId);
        blockingStub.addInitedPartitionedVSet(req.build());
    }

    public float pushDiskAccessForV(float[] diskAccessForV,int insertI,long j){
        InsertjIntoViMessage.Builder message=InsertjIntoViMessage.newBuilder();
        FListMessage fListMessage=MessageDataTransUtil.FloatArray_2_FListMessage(diskAccessForV);
        message.setDiskTimeArray(fListMessage);
        message.setInsertI(insertI);
        message.setJ(j);
        FMessage f=blockingStub.pushDiskAccessForV(message.build());
        return f.getF();
    }

    public LSetListArrayMessage getLsPartitionedVSet(){
        SMessage.Builder sMessage=SMessage.newBuilder();
        try {
            sMessage.setStr(Inet4Address.getLocalHost().getHostName());
        }catch (UnknownHostException e){
            e.printStackTrace();
        }
        return blockingStub.getLsPartitionedVSet(sMessage.build());

    }

    public void putLsPartitionedVSet(LSetListArrayMessage lSetListArrayMessage){
        blockingStub.putLsPartitionedVSet(lSetListArrayMessage);
    }




}