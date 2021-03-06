package Algotithm;

import Jama.Matrix;
import Util.*;
import com.mkobos.pca_transform.PCA;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import context.Context;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;

import javafx.concurrent.Worker;
import net.*;
import org.iq80.leveldb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import visual.Ui;
import visual.UiClient;


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
        long start = System.currentTimeMillis();
//        boolean isExec=false;
//        boolean isExec1=false;
//        boolean isExec2=false;
//        boolean isExec3=false;
//        long a1=0;
//        long a2=0;
//        long a3=0;
//        long a4=0;
        for (int i = 0; i < echo; i++) {
            // 该层是循环所有的batch
            for (int j = 0; j < WorkerContext.sampleBatchListSize; j++) {
                long start_batch=System.currentTimeMillis();
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


                outputValueOfBatch = getActivateValue(batch, paramsMap);
                errorOfBatch = getError(outputValueOfBatch, batch);
                gradientMap = getGradientMap(errorOfBatch, batch, paramsMap);
                // 将gradient发送给server，然后得到新的params
                WorkerContext.psRouterClient.sendGradientMap(gradientMap);

                loss = calculateLoss(outputValueOfBatch, batch) / WorkerContext.sampleBatchSize;
                if(WorkerContext.workerId==Context.masterId){
                    UiClient.ins().plot("loss", Math.abs(loss), j + i * WorkerContext.sampleBatchListSize);
                    plotScatter(batch, errorOfBatch);
                }


                WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getBlockingStub().sendLoss(LossMessage.newBuilder()
                        .setLoss(loss)
                        .setReqHost(WorkerContext.workerId)
                        .setStartTime(start).build());
//                if(Math.abs(loss)<0.65f&&!isExec){
//                    a1=System.currentTimeMillis();
//                    System.out.println("训练时间："+(System.currentTimeMillis()-start));
//                    isExec=true;
//                }
//                if(Math.abs(loss)<0.6f && !isExec1){
//                    a2=System.currentTimeMillis();
//                    System.out.println("训练时间："+(System.currentTimeMillis()-start));
//                    isExec1=true;
//                }
//                if(Math.abs(loss)<0.55f && !isExec2){
//                    a3=System.currentTimeMillis();
//                    System.out.println("训练时间："+(System.currentTimeMillis()-start));
//                    isExec2=true;
//                }
//                if(Math.abs(loss)<0.5f && !isExec3){
//                    a4=System.currentTimeMillis();
//                    System.out.println("训练时间："+(System.currentTimeMillis()-start));
//                    isExec3=true;
//                }

                System.out.println("error echo:" + i + ",batch:" + j + ",loss:" + loss);
                UiClient.ins().plotWorkerProcess(WorkerContext.workerId,i*WorkerContext.sampleBatchListSize+j);

                paramsMap.clear();

                long end_batch=System.currentTimeMillis();
                System.out.println("batchtime:"+(end_batch-start_batch));

            }
//            System.out.println("训练时间："+(a1-start));
//            System.out.println("训练时间："+(a2-start));
//            System.out.println("训练时间："+(a3-start));
//            System.out.println("训练时间："+(a4-start));
        }


        System.out.println("zongshijian:" + totalTime);
    }

    public void plotScatter(SampleList batch, float[] error) {
        int count = 0;
        for (int i = 0; i < error.length; i++) {
            if (Math.abs(error[i]) < 0.5) {
                count++;
            }
        }
        Matrix trainingData = new Matrix(new double[][]{
                {1, 2, 3, 4, 5, 6, 1, 2, 2, 3},
                {6, 5, 4, 3, 2, 1, 7, 4, 2, 3},
                {2, 2, 2, 2, 2, 2, 2, 6, 2, 3}
        });

        PCA pca = new PCA(trainingData);
        double[][] d=new double[batch.sampleList.size()][Context.featureSize];
        for(int i=0;i<batch.sampleList.size();i++){
            for(int j=0;j<Context.featureSize;j++){
                d[i][j]=(double) batch.sampleList.get(i).feature[j];
            }
        }

        Matrix testData = new Matrix(d);
        // 分别表示正确分类的xy，和错误分类的xy
        List<Double> l1x=new ArrayList<Double>();
        List<Double> l1y=new ArrayList<Double>();
        List<Double> l2x=new ArrayList<Double>();
        List<Double> l2y=new ArrayList<Double>();
        // 用来存储上述list
        Map<String,List<Double>> map=new HashMap<String, List<Double>>();

        // 下面做可视化的
//        Matrix transformedData = pca.transform(testData, PCA.TransformationType.WHITENING);
//        for (int r = 0; r < transformedData.getRowDimension(); r++) {
//                if(Math.abs(error[r])<0.5){
//                    // 正确分类
//                    l1x.add(transformedData.get(r,0));
//                    l1y.add(transformedData.get(r,1));
//                }else {
//                    l2x.add(transformedData.get(r,0));
//                    l2y.add(transformedData.get(r,1));
//                }
//        }
//        map.put("l1x",l1x);
//        map.put("l1y",l1y);
//        map.put("l2x",l2x);
//        map.put("l2y",l2y);
//
//        UiClient.ins().plotScatterGraph("LoRScatter",map);
        System.out.println("count:" + count);
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
            if(sample.feature!=null){
                for (int j = 0; j < sample.feature.length; j++) {
                    if (sample.feature[j] != -1) {
                        if (map.get("f" + j) != null) {
                            float curGradient = map.get("f" + j);
                            curGradient += learningRate * error[i] * sample.feature[j] - l2Lambda * paramsMap.get("f" + j);
                            map.put("f" + j, curGradient);
                        } else {
                            map.put("f" + j, learningRate * error[i] * sample.feature[j] - l2Lambda * paramsMap.get("f" + j));
                        }
                    }

                }
            }
            if(sample.cat!=null){
                for (int j = 0; j < sample.cat.length; j++) {
                    if (sample.cat[j] != -1) {
                        if (map.get("p" + sample.cat[j]) != null) {
                            float curGradient = map.get("p" + sample.cat[j]);
                            curGradient += learningRate * error[i] - l2Lambda * paramsMap.get("p" + sample.cat[j]);
                            map.put("p" + j, curGradient);
                        } else {
                            map.put("p" + sample.cat[j], learningRate * error[i] - l2Lambda * paramsMap.get("p" + sample.cat[j]));
                        }
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
            if(sample.feature!=null){
                for (int i = 0; i < sample.feature.length; i++) {
                    if (sample.feature[i] != -1) {
                        value[l] += sample.feature[i] * paramsMap.get("f" + i);
                    }
                }
            }
            if(sample.cat!=null){
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