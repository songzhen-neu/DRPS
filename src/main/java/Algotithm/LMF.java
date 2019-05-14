package Algotithm;

import Util.MessageDataTransUtil;
import Util.TypeExchangeUtil;
import context.Context;
import context.WorkerContext;
import dataStructure.SparseMatrix.Matrix;
import dataStructure.SparseMatrix.MatrixElement;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import net.IMessage;
import net.PSWorker;
import net.SFKVListMessage;
import net.SRListMessage;
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
    /**
     * 一个echo执行一遍数据集
     */
    int echo;
    /**
     * 矩阵的秩
     */
    int rank;

    /**
     * 用户数
     */
    long userNum;
    /**
     * 电影数
     */
    long movieNum;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    // 矩阵分解，这里用key-value表示稀疏矩阵，那么矩阵元素(i,j)的key就是i*colNum+j
    // 那么取出来的时候，就是[key/colNum,key%colNum]

    public LMF(float lambda, float learningRate, int echo, int rank, long userNum, long movieNum) {
        this.lambda = lambda;
        this.learningRate = learningRate;
        this.echo = echo;
        this.rank = rank;
        this.userNum = userNum;
        this.movieNum = movieNum;
    }

    public void train() throws IOException, ClassNotFoundException {
        // 首先是获取sampleBatch
        DB db = WorkerContext.kvStoreForLevelDB.getDb();
        List<PSWorker> psRouterClient = WorkerContext.psRouterClient.getPsWorkers();
        Map<String, Float[]>[] paramsMapsTemp = new HashMap[Context.serverNum];
        Map<String, Float[]> paramsMap = new HashMap<String, Float[]>();
        float[] outputValueOfBatch;
        float[] errorOfBatch;
        Map<String, Float[]> gradientMap;
        float loss = 0;


        // 构建paramAssignedToServerMap,用来判断参数应该分给那个server
        WorkerContext.kvStoreForLevelDB.paramAssignedToServerMap = new HashMap<Long, Integer>();
        Set<Long>[] vSet = WorkerContext.kvStoreForLevelDB.getVSet();
        // l和i都是长整型,不是string类型，paramAssignedToServerMap是确定哪些参数分到哪个server的请求中
        for (int i = 0; i < vSet.length; i++) {
            for (Long l : vSet[i]) {
                WorkerContext.kvStoreForLevelDB.paramAssignedToServerMap.put(l, i);
            }
        }

        // 先把要执行的次数发给本地的server
        WorkerContext.psRouterClient.getPsWorkers().get(WorkerContext.workerId).getBlockingStub()
                .sendTrainRoundNum(IMessage.newBuilder().setI(WorkerContext.sampleBatchListSize_LMF * echo).build());
        // 该层是循环echo遍数据集
        long totalTime = 0;
        for (int i = 0; i < echo; i++) {
            // 该层是循环所有的batch
            for (int j = 0; j < WorkerContext.sampleBatchListSize_LMF; j++) {
                Matrix batch = (Matrix) TypeExchangeUtil.toObject(db.get(("batchLMF" + j).getBytes()));
                // 根据sampleBatch得到训练需要用到的参数
                Set<String>[] setArray = getNeededParamsKey(batch);

                // 下面可以向server发送请求了
                logger.info("echo " + i + ":Sent request of Params to servers");
                Future<SRListMessage> srListMessageFuture[] = new Future[Context.workerNum];


                for (int l = 0; l < Context.serverNum; l++) {
//                    if (setArray[l].size() != 0) {
                    logger.info("getNeededParams start");
//                    CurrentTimeUtil.setStartTime();
                    srListMessageFuture[l] = psRouterClient.get(l).getNeededParamsLMF(setArray[l],
                            WorkerContext.workerId,
                            WorkerContext.sampleBatchListSize * (i) + j + 1);
//                    CurrentTimeUtil.setEndTime();
//                    CurrentTimeUtil.showExecuteTime("获取一次参数的时间");
                    logger.info("getNeededParams end");
//                    }
                }
                for (int l = 0; l < Context.serverNum; l++) {
                    while (!srListMessageFuture[l].isDone()) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        paramsMapsTemp[l] = MessageDataTransUtil.SRListMessage_2_Map(srListMessageFuture[l].get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    for (String key : paramsMapsTemp[l].keySet()) {
                        // 把远程请求到的参数放到paramsMap里，这里内存是可以存得下一个batch的参数的
                        paramsMap.put(key, paramsMapsTemp[l].get(key));
                    }
                }

                // 接下来是计算更新。根据batch
                // getErrorValue是长度为batchSize的float数组，里面存的是每一个样本的误差
                errorOfBatch = getErrorValue(batch, paramsMap);
                gradientMap = getGradientMap(errorOfBatch, batch, paramsMap);
//                // 将gradient发送给server，然后得到新的params
                WorkerContext.psRouterClient.sendGradientMapLMF(gradientMap);
//
                loss = calculateLoss(errorOfBatch) ;
//
                System.out.println("error echo:" + i + ",batch:" + j + ",loss:" + loss);
//
                paramsMap.clear();


            }
        }
        System.out.println("zongshijian:" + totalTime);
    }

    public float calculateLoss(float[] error) {
        float loss = 0;
        for (int i = 0; i < error.length; i++) {
            loss += error[i];
        }
        return loss;
    }


    public Map<String, Float[]> getGradientMap(float[] error, Matrix batch, Map<String, Float[]> paramsMap) {
        // 梯度的数据结构，p100表示u第100行，p1000表示v的第57列（1000-943）
        Map<String, Float[]> map = new HashMap<String, Float[]>();
        long userNum=WorkerContext.userNum_LMF;
        long movieNum=WorkerContext.movieNum_LMF;

        // 对于每个batch都计算梯度
        for(MatrixElement matrixElement:batch.matrix){
            // error[i]表示batch中第i条数据的误差值
            // 当前数据是batch中第n条数据
            int n=batch.matrix.indexOf(matrixElement);
            // 当前要更新的是U的第row行和V的第col列
            long row=matrixElement.row;
            long col=matrixElement.col;

            if(!map.keySet().contains("p"+row)&&!map.keySet().contains("p"+(userNum+col))){
                // matrixElement是一个评分值，有行列和value
                // 要更新U的所有row行，V的所有col列
                Float[] uGrad=initFloatArray(rank);
                Float[] vGrad=initFloatArray(rank);
                for(int i=0;i<rank;i++){
                    // U的第row行第i列的更新为
                    uGrad[i]+=error[n]*paramsMap.get("p"+(userNum+col))[i];
                    vGrad[i]+=error[n]*paramsMap.get("p"+row)[i];
                }
                map.put("p"+row,uGrad);
                map.put("p"+(userNum+col),vGrad);
            }else if(!map.keySet().contains("p"+row)&&map.keySet().contains("p"+(userNum+col))){
                Float[] uGrad=initFloatArray(rank);
                Float[] vGrad=map.get("p"+(userNum+col));
                for(int i=0;i<rank;i++){
                    // U的第row行第i列的更新为
                    uGrad[i]+=error[n]*paramsMap.get("p"+(userNum+col))[i];
                    vGrad[i]+=error[n]*paramsMap.get("p"+row)[i];
                }
                map.put("p"+row,uGrad);
                map.put("p"+(userNum+col),vGrad);
            }else if(map.keySet().contains("p"+row)&&!map.keySet().contains("p"+(userNum+col))){
                Float[] uGrad=map.get("p"+row);
                Float[] vGrad=initFloatArray(rank);
                for(int i=0;i<rank;i++){
                    // U的第row行第i列的更新为
                    uGrad[i]+=error[n]*paramsMap.get("p"+(userNum+col))[i];
                    vGrad[i]+=error[n]*paramsMap.get("p"+row)[i];
                }
                map.put("p"+row,uGrad);
                map.put("p"+(userNum+col),vGrad);
            }else {
                Float[] uGrad=map.get("p"+row);
                Float[] vGrad=map.get("p"+(userNum+col));
                for(int i=0;i<rank;i++){
                    // U的第row行第i列的更新为
                    uGrad[i]+=error[n]*paramsMap.get("p"+(userNum+col))[i];
                    vGrad[i]+=error[n]*paramsMap.get("p"+row)[i];
                }
                map.put("p"+row,uGrad);
                map.put("p"+(userNum+col),vGrad);
            }

        }

        for(String str:map.keySet()){
            for(int i=0;i<rank;i++){
                map.get(str)[i]=(learningRate*lambda)*paramsMap.get(str)[i]-learningRate*map.get(str)[i];
            }
        }




        return map;
    }



    public Float[] initFloatArray(int rank){
        Float[] floats=new Float[rank];
        for(int i=0;i<floats.length;i++){
            floats[i]=new Float(0);
        }
        return floats;
    }

    public float[] getErrorValue(Matrix batch, Map<String, Float[]> paramsMap) {
        float value[] = new float[batch.matrix.size()];
        for (int l = 0; l < value.length; l++) {
            MatrixElement matrixElement = batch.matrix.get(l);
            Float[] row=paramsMap.get("p"+matrixElement.row);
            Float[] col=paramsMap.get("p"+(WorkerContext.userNum_LMF+matrixElement.col));
            float prod=0;
            for(int i=0;i<row.length;i++){
                prod+=row[i]*col[i];
            }
            value[l]=Math.abs(matrixElement.val-prod);

        }
        return value;

    }

    public Set<String>[] getNeededParamsKey(Matrix batch) {

        Set[] setArray = new HashSet[Context.serverNum];
        Map<Long, Integer> paramAssignedToServerMap = WorkerContext.kvStoreForLevelDB.paramAssignedToServerMap;
        for (int i = 0; i < setArray.length; i++) {
            setArray[i] = new HashSet<String>();
        }

        for (MatrixElement matrixElement : batch.matrix) {


            // 判断l是否包含在vSet参数划分里，如果包含在，那么通过vset进行参数路由
            // 一个评分需要在U中取一行，在V中取一列，那么一种四种情况UV、U、V、null


            if (paramAssignedToServerMap.keySet().contains(matrixElement.row)&&
                    paramAssignedToServerMap.keySet().contains(WorkerContext.userNum_LMF+matrixElement.col)) {
                setArray[paramAssignedToServerMap.get(matrixElement.row)].add("p" + matrixElement.row);
                setArray[paramAssignedToServerMap.get(matrixElement.col+WorkerContext.userNum_LMF)].add("p" + (matrixElement.col+WorkerContext.userNum_LMF));
            }else if(paramAssignedToServerMap.keySet().contains(matrixElement.row)&&
                    !paramAssignedToServerMap.keySet().contains(WorkerContext.userNum_LMF+matrixElement.col)){
                setArray[paramAssignedToServerMap.get(matrixElement.row)].add("p" + matrixElement.row);
                setArray[(int)(matrixElement.col+WorkerContext.userNum_LMF)%Context.serverNum].add("p" + (matrixElement.col+WorkerContext.userNum_LMF));
            }else if(paramAssignedToServerMap.keySet().contains(WorkerContext.userNum_LMF+matrixElement.col)&&
                    !paramAssignedToServerMap.keySet().contains(matrixElement.row)){
                setArray[(int)matrixElement.row%Context.serverNum].add("p" + matrixElement.row);
                setArray[paramAssignedToServerMap.get(matrixElement.col+WorkerContext.userNum_LMF)].add("p" + (matrixElement.col+WorkerContext.userNum_LMF));
            }else {
                setArray[(int)matrixElement.row%Context.serverNum].add("p" + matrixElement.row);
                setArray[(int)(matrixElement.col+WorkerContext.userNum_LMF)%Context.serverNum].add("p" + (matrixElement.col+WorkerContext.userNum_LMF));
            }



        }
        return setArray;

    }
}