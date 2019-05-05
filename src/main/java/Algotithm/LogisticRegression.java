package Algotithm;

import Util.DataProcessUtil;
import Util.MessageDataTransUtil;
import Util.TypeExchangeUtil;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import context.Context;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;

import javafx.concurrent.Worker;
import net.IMessage;
import net.PSRouterClient;
import net.PSWorker;
import net.SFKVListMessage;
import org.iq80.leveldb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @program: simplePsForModelPartition
 * @description: 逻辑回归算法
 * @author: SongZhen
 * @create: 2018-12-19 14:56
 */
public class LogisticRegression {
    float l2Lambda;
    float learningRate;
    int echo;// 表示迭代n次训练集
    Logger logger = LoggerFactory.getLogger(this.getClass());


    public LogisticRegression(float l2Lambda, float learningRate, int echo) {
        this.l2Lambda = l2Lambda;
        this.learningRate = learningRate;
        this.echo = echo;
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

        // 先把要执行的次数发给本地的server
        WorkerContext.psRouterClient.getPsWorkers().get(WorkerContext.workerId).getBlockingStub()
                .sendTrainRoundNum(IMessage.newBuilder().setI(WorkerContext.sampleBatchListSize*echo).build());
        // 该层是循环echo遍数据集
        for (int i = 0; i < echo; i++) {
            // 该层是循环所有的batch
            for (int j = 0; j < WorkerContext.sampleBatchListSize; j++) {
                SampleList batch = (SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch" + j).getBytes()));
                // 根据sampleBatch得到训练需要用到的参数
                Set<String>[] setArray = getNeededParamsKey(batch);

                // 下面可以向server发送请求了
                logger.info("echo " + i + ":Sent request of Params to servers");
                Future<SFKVListMessage> sfkvListMessageFuture[]=new Future[Context.workerNum];
                for (int l = 0; l < Context.serverNum; l++) {
                    if (setArray[l].size() != 0) {
                        logger.info("getNeededParams start");
                        sfkvListMessageFuture[l]=psRouterClient.get(l).getNeededParams(setArray[l],
                                WorkerContext.workerId,
                                WorkerContext.sampleBatchListSize*(i)+j+1 );
                        logger.info("getNeededParams end");
                    }
                }
                for(int l=0;l<Context.serverNum;l++){
                    logger.info(l+"barrier start");
                    while (!sfkvListMessageFuture[l].isDone()){
                        try{
                            Thread.sleep(10);
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    logger.info(l+"barrier end");
                    try{
                        paramsMapsTemp[l] = MessageDataTransUtil.SFKVListMessage_2_Map(sfkvListMessageFuture[l].get());
                    }catch (InterruptedException|ExecutionException e){
                        e.printStackTrace();
                    }

                    for (String key : paramsMapsTemp[l].keySet()) {
                        // 把远程请求到的参数放到paramsMap里，这里内存是可以存得下一个batch的参数的
                        paramsMap.put(key, paramsMapsTemp[l].get(key));
                    }
                }


                outputValueOfBatch = getActivateValue(batch, paramsMap);
                errorOfBatch = getError(outputValueOfBatch, batch);
                gradientMap = getGradientMap(errorOfBatch, batch, paramsMap);
                // 将gradient发送给server，然后得到新的params
                WorkerContext.psRouterClient.sendGradientMap(gradientMap);

                loss = calculateLoss(outputValueOfBatch, batch) / WorkerContext.sampleBatchSize;

                System.out.println("error echo:" + i + ",batch:" + j + ",loss:" + loss);

                paramsMap.clear();


            }
        }
    }

    public float calculateLoss(float[] outputValueOfBatch, SampleList batch) {
        float loss = 0;
        for (int i = 0; i < batch.sampleList.size(); i++) {
            loss += (batch.sampleList.get(i).click * Math.log(outputValueOfBatch[i]) + (1 - batch.sampleList.get(i).click) * Math.log(1 - outputValueOfBatch[i]));
        }
        return loss;
    }


    public Map<String, Float> getGradientMap(float[] error, SampleList batch, Map<String, Float> paramsMap) {
        Map<String, Float> map = new HashMap<String, Float>();
        for (int i = 0; i < batch.sampleList.size(); i++) {
            Sample sample = batch.sampleList.get(i);
            for (int j = 0; j < sample.feature.length; j++) {
                if (sample.feature[j] != -1) {
                    if (map.get("featParam" + j) != null) {
                        float curGradient = map.get("featParam" + j);
                        curGradient += learningRate * error[i] * sample.feature[j] - l2Lambda * paramsMap.get("featParam" + j);
                        map.put("featParam" + j, curGradient);
                    } else {
                        map.put("featParam" + j, learningRate * error[i] * sample.feature[j] - l2Lambda * paramsMap.get("featParam" + j));
                    }
                }

            }

            for (int j = 0; j < sample.cat.length; j++) {
                if (sample.cat[j] != -1) {
                    if (map.get("catParam" + sample.cat[j]) != null) {
                        float curGradient = map.get("catParam" + sample.cat[j]);
                        curGradient += learningRate * error[i] - l2Lambda * paramsMap.get("catParam" + sample.cat[j]);
                        map.put("catParam" + j, curGradient);
                    } else {
                        map.put("catParam" + sample.cat[j], learningRate * error[i] - l2Lambda * paramsMap.get("catParam" + sample.cat[j]));
                    }
                }

            }
        }

        for (String key : map.keySet()) {
            map.put(key, map.get(key) / WorkerContext.sampleBatchSize);
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

    public float[] getActivateValue(SampleList batch, Map<String, Float> paramsMap) {
        float value[] = new float[batch.sampleList.size()];
        for (int l = 0; l < value.length; l++) {
            Sample sample = batch.sampleList.get(l);
            // 计算feature的value
            for (int i = 0; i < sample.feature.length; i++) {
                if (sample.feature[i] != -1) {
                    value[l] += sample.feature[i] * paramsMap.get("featParam" + i);
                }
            }
            for (int i = 0; i < sample.cat.length; i++) {
                if (sample.cat[i] != -1) {
//                    System.out.println(sample.cat[i]);
//                    if(paramsMap.get("catParam" + sample.cat[i])==null){
//                        System.out.println("hakong");
//                    }
                    value[l] += paramsMap.get("catParam" + sample.cat[i]);
                }
            }

            // 下面对value做处理
            if (value[l] >= 10) {
                value[l] = 1;
            } else if (value[l] <= -10) {
                value[l] = 0;
            } else {
                value[l] = (float) (1.0f / (1 + Math.exp(-value[l])));
            }
        }
        return value;

    }

    public Set<String>[] getNeededParamsKey(SampleList batch) {
        Set[] setArray = new HashSet[Context.serverNum];
        Set[] vSet = WorkerContext.kvStoreForLevelDB.getVSet();
        for (int i = 0; i < setArray.length; i++) {
            setArray[i] = new HashSet<String>();
        }

        for (Sample sample : batch.sampleList) {
            for (Long l : sample.cat) {
                boolean isContains=false;
                if (!l.equals(-1l)) {
                    // 判断l是否包含在vSet参数划分里，如果包含在，那么通过vset进行参数路由
                    for (int i = 0; i < vSet.length; i++) {
                        if (vSet[i].contains(l)) {
                            setArray[i].add("catParam" + l);
                            isContains = true;
                            break;
                        }
                    }

                    // 如果不包含，说明并未进行参数划分，按照正常的取余进行分配
                    if (!isContains) {
                        setArray[l.intValue() % Context.serverNum].add("catParam" + l);
                    }

                }

            }
        }
        return setArray;

    }
}