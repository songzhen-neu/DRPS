package Algotithm;

import Util.AlgorithmUtil;
import Util.MessageDataTransUtil;
import Util.TypeExchangeUtil;
import context.Context;
import context.WorkerContext;
import dataStructure.SparseMatrix.Matrix;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import io.grpc.stub.StreamObserver;
import net.IMessage;
import net.PSWorker;
import net.SFKVListMessage;
import org.iq80.leveldb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SVM {
    float lambda;
    float learningRate;
    int echo;// 表示迭代n次训练集
    Logger logger = LoggerFactory.getLogger(this.getClass());
    static float cost=0;


    public SVM(float learningRate, float labda, int echo) {
        this.learningRate = learningRate;
        this.echo = echo;
        this.lambda = labda;
    }

    public void train() throws IOException, ClassNotFoundException {
        // 首先是获取sampleBatch
        DB db = WorkerContext.kvStoreForLevelDB.getDb();
        List<PSWorker> psRouterClient = WorkerContext.psRouterClient.getPsWorkers();
        Map<String, Float>[] paramsMapsTemp = new HashMap[Context.serverNum];
        Map<String, Float> paramsMap = new HashMap<String, Float>();
        float[] outputValueOfBatch;
        float[] errorOfBatch;
        Map<String, Float> gradientMap;
        float loss = 0;


        // 构建paramAssignedToServerMap
        WorkerContext.kvStoreForLevelDB.paramAssignedToServerMap = new HashMap<Long, Integer>();
        Set<Long>[] vSet = WorkerContext.kvStoreForLevelDB.getVSet();
        for (int i = 0; i < vSet.length; i++) {
            for (Long l : vSet[i]) {
                WorkerContext.kvStoreForLevelDB.paramAssignedToServerMap.put(l, i);
            }
        }

        // 先把要执行的次数发给本地的server
        WorkerContext.psRouterClient.getPsWorkers().get(WorkerContext.workerId).getBlockingStub()
                .sendTrainRoundNum(IMessage.newBuilder().setI(WorkerContext.sampleBatchListSize * echo).build());
        // 该层是循环echo遍数据集
        long totalTime = 0;
        for (int i = 0; i < echo; i++) {
            // 该层是循环所有的batch
            for (int j = 0; j < WorkerContext.sampleBatchListSize; j++) {
                SampleList batch = (SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch" + j).getBytes()));
                // 根据sampleBatch得到训练需要用到的参数
                Set<String>[] setArray = getNeededParamsKey(batch);

                // 下面可以向server发送请求了
                logger.info("echo " + i + ":Sent request of Params to servers");
                Future<SFKVListMessage> sfkvListMessageFuture[] = new Future[Context.workerNum];

//                long startTime = System.currentTimeMillis();


                for (int l = 0; l < Context.serverNum; l++) {
//                    if (setArray[l].size() != 0) {
                    logger.info("getNeededParams start");
//                    CurrentTimeUtil.setStartTime();
                    sfkvListMessageFuture[l] = psRouterClient.get(l).getNeededParams(setArray[l],
                            WorkerContext.workerId,
                            WorkerContext.sampleBatchListSize * (i) + j + 1);
//                    CurrentTimeUtil.setEndTime();
//                    CurrentTimeUtil.showExecuteTime("获取一次参数的时间");
                    logger.info("getNeededParams end");
//                    }
                }
                AlgorithmUtil.getParamsMap(paramsMapsTemp, paramsMap, sfkvListMessageFuture, logger);
//                long endTime = System.currentTimeMillis();
//                totalTime += (endTime - startTime);



                // 这是取的batch中每条数据的计算出来的预测值
                outputValueOfBatch = getActivateValue_svm(batch, paramsMap);
                gradientMap = getGradientMap(outputValueOfBatch, batch, paramsMap);
                // 将gradient发送给server，然后得到新的params
                WorkerContext.psRouterClient.sendGradientMap(gradientMap);

                System.out.println("error echo:" + i + ",batch:" + j + ",loss:" + cost);
                cost=0;



                paramsMap.clear();


            }
        }
        System.out.println("zongshijian:" + totalTime);
    }


    public float calculateLoss(float[] outputValueOfBatch, SampleList batch) {
        float loss = 0;
        for (int i = 0; i < batch.sampleList.size(); i++) {
            loss += (batch.sampleList.get(i).click * Math.log(outputValueOfBatch[i]) + (1 - batch.sampleList.get(i).click) * Math.log(1 - outputValueOfBatch[i]));
        }
        return loss;
    }


    public Map<String, Float> getGradientMap(float[] outputValue, SampleList batch, Map<String, Float> paramsMap) {
        Map<String, Float> map = new HashMap<String, Float>();
        float cost = 0;
        // 先把所有维度都变成原来的lamda倍的绝对值
        for(String str:paramsMap.keySet()){
            map.put(str,Math.abs(paramsMap.get(str)*lambda));
        }


        for (int i = 0; i < batch.sampleList.size(); i++) {
            Sample sample = batch.sampleList.get(i);
            // 对于每条数据的每个feature属性（第i条数据的第j个feature属性）

            // 第f_j维的梯度
            if (sample.click * outputValue[i] - 1 < 0) {
                cost += (1 - sample.click * outputValue[i]);
            }

        }

        for(String s:paramsMap.keySet()) {
            cost += 0.5 * lambda * paramsMap.get(s)*paramsMap.get(s);
        }

        for(int i=0;i<batch.sampleList.size();i++){
            Sample sample=batch.sampleList.get(i);
            for(int j=0;j<sample.feature.length;j++){
                if(outputValue[i]*sample.click-1<0){
                    map.put("f"+j,(map.get("f"+j)-sample.click*sample.feature[j]));
                }
            }

            for(int j=0;j<sample.cat.length;j++){
                if(outputValue[i]*sample.click-1<0){
                    map.put("p"+sample.cat[j],(map.get("p"+sample.cat[j])-sample.click));
                }
            }
        }



        this.cost=cost;

        for (String key : map.keySet()) {
            map.put(key, -map.get(key)*learningRate);
        }
        return map;
    }


    public float[] getError(float[] outputValueBatch, SampleList batch) {
        float[] error = new float[batch.sampleList.size()];
        for (int i = 0; i < error.length; i++) {
            error[i] = (batch.sampleList.get(i).click - outputValueBatch[i]);

        }
        return error;
    }

    public float[] getActivateValue_svm(SampleList batch, Map<String, Float> paramsMap) {
        float value[] = new float[batch.sampleList.size()];
        AlgorithmUtil.getActivateValue(batch, paramsMap, value);
        return value;

    }


    public Set<String>[] getNeededParamsKey(SampleList batch) {
        Set[] setArray = new HashSet[Context.serverNum];
        Map<Long, Integer> paramAssignedToServerMap = WorkerContext.kvStoreForLevelDB.paramAssignedToServerMap;
        for (int i = 0; i < setArray.length; i++) {
            setArray[i] = new HashSet<String>();
        }

        for (Sample sample : batch.sampleList) {
            for (Long l : sample.cat) {
                boolean isContains = false;
                if (!l.equals(-1l)) {
                    // 判断l是否包含在vSet参数划分里，如果包含在，那么通过vset进行参数路由
                    if (paramAssignedToServerMap.keySet().contains(l)) {
                        setArray[paramAssignedToServerMap.get(l)].add("p" + l);
                        isContains = true;
                    }
                    // 如果不包含，说明并未进行参数划分，按照正常的取余进行分配
                    if (!isContains) {
                        setArray[l.intValue() % Context.serverNum].add("p" + l);
                    }

                }

            }
        }
        return setArray;

    }
}
