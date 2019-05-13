package Algotithm;

import Util.MessageDataTransUtil;
import Util.TypeExchangeUtil;
import context.Context;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
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

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-13 14:42
 */
public class LMF {
    float lambda;
    float learningRate;
    /** 一个echo执行一遍数据集*/
    int echo;
    /** 矩阵的秩*/
    int rank;

    /** 用户数*/
    int userNum;
    /** 电影数*/
    int movieNum;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    // 矩阵分解，这里用key-value表示稀疏矩阵，那么矩阵元素(i,j)的key就是i*colNum+j
    // 那么取出来的时候，就是[key/colNum,key%colNum]

    public LMF(float lambda, float learningRate, int echo, int rank,int userNum,int movieNum) {
        this.lambda = lambda;
        this.learningRate = learningRate;
        this.echo = echo;
        this.rank=rank;
        this.userNum=userNum;
        this.movieNum=movieNum;
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


        // 构建paramAssignedToServerMap,用来判断参数应该分给那个server
        WorkerContext.kvStoreForLevelDB.paramAssignedToServerMap = new HashMap<Long, Integer>();
        Set<Long>[] vSet = WorkerContext.kvStoreForLevelDB.getVSet();
        // l和i都是长整型
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
                for (int l = 0; l < Context.serverNum; l++) {
                    while (!sfkvListMessageFuture[l].isDone()) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        paramsMapsTemp[l] = MessageDataTransUtil.SFKVListMessage_2_Map(sfkvListMessageFuture[l].get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    for (String key : paramsMapsTemp[l].keySet()) {
                        // 把远程请求到的参数放到paramsMap里，这里内存是可以存得下一个batch的参数的
                        paramsMap.put(key, paramsMapsTemp[l].get(key));
                    }
                }



                outputValueOfBatch = getOutputValue(batch, paramsMap);
                errorOfBatch = getError(outputValueOfBatch, batch);
                gradientMap = getGradientMap(errorOfBatch, batch, paramsMap);
                // 将gradient发送给server，然后得到新的params
                WorkerContext.psRouterClient.sendGradientMap(gradientMap);

                loss = calculateLoss(outputValueOfBatch, batch) / WorkerContext.sampleBatchSize;

                System.out.println("error echo:" + i + ",batch:" + j + ",loss:" + loss);

                paramsMap.clear();


            }
        }
        System.out.println("zongshijian:" + totalTime);
    }

    public float calculateLoss(float[] outputValueOfBatch, SampleList batch) {
        float loss = 0;
        for (int i = 0; i < batch.sampleList.size(); i++) {
            loss += outputValueOfBatch[i]-batch.sampleList.get(i).click;
        }
        return loss;
    }


    public Map<String, Float> getGradientMap(float[] error, SampleList batch, Map<String, Float> paramsMap) {
        Map<String, Float> map = new HashMap<String, Float>();
        for (int i = 0; i < batch.sampleList.size(); i++) {
            Sample sample = batch.sampleList.get(i);
            for (int j = 0; j < sample.feature.length; j++) {
                if (sample.feature[j] != -1) {
                    if (map.get("f" + j) != null) {
                        float curGradient = map.get("f" + j);
                        curGradient += learningRate * error[i] * sample.feature[j] ;
                        map.put("f" + j, curGradient);
                    } else {
                        map.put("f" + j, learningRate * error[i] * sample.feature[j] );
                    }
                }

            }

            for (int j = 0; j < sample.cat.length; j++) {
                if (sample.cat[j] != -1) {
                    if (map.get("p" + sample.cat[j]) != null) {
                        float curGradient = map.get("p" + sample.cat[j]);
                        curGradient += learningRate * error[i]*paramsMap.get("p" + sample.cat[j]);
                        map.put("p" + j, curGradient);
                    } else {
                        map.put("p" + sample.cat[j], learningRate * error[i]* paramsMap.get("p" + sample.cat[j]));
                    }
                }

            }
        }


        return map;
    }


    public float[] getError(float[] outputValueBatch, SampleList batch) {
        float[] error = new float[batch.sampleList.size()];
        for (int i = 0; i < error.length; i++) {
            // 这里的click代表label，待会把所有的click换成label即可
            error[i] = (batch.sampleList.get(i).click - outputValueBatch[i]);

        }
        return error;
    }

    public float[] getOutputValue(SampleList batch, Map<String, Float> paramsMap) {
        float value[] = new float[batch.sampleList.size()];
        for (int l = 0; l < value.length; l++) {
            Sample sample = batch.sampleList.get(l);
            // 计算feature的value
            for (int i = 0; i < sample.feature.length; i++) {
                if (sample.feature[i] != -1) {
                    value[l] += sample.feature[i] * paramsMap.get("f" + i);
                }
            }
            for (int i = 0; i < sample.cat.length; i++) {
                if (sample.cat[i] != -1) {
//                    System.out.println(sample.cat[i]);
//                    if(paramsMap.get("catParam" + sample.cat[i])==null){
//                        System.out.println("hakong");
//                    }
                    value[l] += paramsMap.get("p" + sample.cat[i]);
                }
            }


        }
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