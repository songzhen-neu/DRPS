package Algotithm;

import Util.TypeExchangeUtil;
import context.Context;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;

import net.PSRouterClient;
import net.PSWorker;
import org.iq80.leveldb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.*;

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
    Logger logger=LoggerFactory.getLogger(this.getClass());


    public LogisticRegression(float l2Lambda,float learningRate,int echo){
        this.l2Lambda=l2Lambda;
        this.learningRate=learningRate;
        this.echo=echo;
    }

    public void train()throws IOException,ClassNotFoundException {
        // 首先是获取sampleBatch
        DB db=WorkerContext.kvStoreForLevelDB.getDb();
        List<PSWorker> psRouterClient=WorkerContext.psRouterClient.getPsWorkers();
        Map<String,Float>[] paramsMapsTemp=new HashMap[Context.serverNum];
        Map<String,Float> paramsMap=new HashMap<String, Float>();



        for(int i=0;i<echo;i++){
            for(int j=0;j<WorkerContext.sampleBatchListSize;j++){
                SampleList batch=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+j).getBytes()));
                // 根据sampleBatch得到训练需要用到的参数
                Set<String>[] setArray=getNeededParamsKey(batch);

                // 下面可以向server发送请求了
                logger.info("echo "+i+":Sent request of Params to servers");
                for(int l=0;l<Context.serverNum;l++){
                    if(setArray[l].size()!=0){
                        paramsMapsTemp[l]=psRouterClient.get(l).getNeededParams(setArray[l]);
                        for(String key:paramsMapsTemp[l].keySet()){
                            paramsMap.put(key,paramsMapsTemp[l].get(key));
                        }
                    }
                }



            }
        }
    }

    public Set<String>[] getNeededParamsKey(SampleList batch){
        Set[] setArray=new HashSet[Context.serverNum];
        for(int i=0;i<setArray.length;i++){
            setArray[i]=new HashSet<String>();
        }

        for(Sample sample:batch.sampleList){
            for(Long i:sample.cat){
                setArray[i.hashCode()%Context.serverNum].add("catParam"+i);
            }
        }
        return setArray;

    }
}