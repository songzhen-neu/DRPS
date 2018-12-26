package Util;


import com.sun.corba.se.spi.orbutil.threadpool.Work;
import context.Context;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import net.IntListMessage;
import net.PSWorker;
import org.iq80.leveldb.DB;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @program: CtrForBigModel
 * @description: 对训练集进行处理
 * @author: SongZhen
 * @create: 2018-11-12 16:38
 */
public class DataProcessUtil {


    public static void metaToDB(String fileName, int featureSize, int catSize) throws IOException,ClassNotFoundException {
        /**
         *@Description: 读取数据
         *@Param: [fileName, featureSize, catSize]
         *@return: ParaStructure.Sample.SampleList
         *@Author: SongZhen
         *@date: 18-11-12
         */


        long freemem = MemoryUtil.getFreeMemory();
        long sparseDimSize=0l;
        SampleList sampleList;

        // 以下是离散特征在前连续特征在后的代码
        if (WorkerContext.isCatForwardFeature) {
//            sparseDimSize=metaDataToSampleLevelDB(fileName, featureSize, catSize);
//            sparseDimSize=metaDataToBatchSampleLevelDB(fileName,featureSize,catSize);
            getSampleBatchListByBatchIndex(fileName,featureSize,catSize);

        } else if (!WorkerContext.isCatForwardFeature) {
            // 下面是连续特征在前，离散特征在后的代码
           System.out.println("方法还没写");
        }


    }


    public static void linerNormalization() throws IOException,ClassNotFoundException{  // 这里标准化是为了提升训练精度。那么也就是(a-amin)/(amax-amin)，一定落在(0,1)区间内
       // 本地统计每个属性最大和最小的feature，然后发给server，server统计全局最大和最小的，返回给worker
        float[] max=new float[Context.featureSize];
        float[] min=new float[Context.featureSize];
        DB db=WorkerContext.kvStoreForLevelDB.getDb();

        // 初始化max和min数组，max为float的最小值，min为float的最大值
        for(int i=0;i<Context.featureSize;i++){
            max[i]=Float.MIN_VALUE;
            min[i]=Float.MAX_VALUE;
        }

        // 遍历所有数据，找到每个feature的最大和最小值
        for(int i=0;i<WorkerContext.sampleBatchListSize;i++){
            SampleList batch=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+i).getBytes()));
            for(int j=0;j<batch.sampleList.size();j++){
                float[] feature=batch.sampleList.get(j).feature;
                for(int k=0;k<feature.length;k++){
                    if(feature[k]>max[k]){
                        max[k]=feature[k];
                    }
                    if(feature[k]<min[k]){
                        min[k]=feature[k];
                    }
                }
            }

        }

        // 将最大和最小值发送给worker
        PSWorker psWorker=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId);
        psWorker.getGlobalMaxMinOfFeature(max,min);


        // 遍历所有数据，对每一条数据的每个sample进行规范化
        for(int i=0;i<WorkerContext.sampleBatchListSize;i++){
            SampleList batch=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+i).getBytes()));
            for(int j=0;j<batch.sampleList.size();j++){
                float[] feature=batch.sampleList.get(j).feature;
                for(int k=0;k<feature.length;k++){
                    feature[k]=(feature[k]-min[k])/(max[k]-min[k]);
                }
            }

            // 把sampleBatch写入数据库
            db.delete(("sampleBatch"+i).getBytes());
            db.put(("sampleBatch"+i).getBytes(),TypeExchangeUtil.toByteArray(batch));

        }



    }

    public static boolean isCatEmpty(String cat){
        if(cat.equals("")){
            return true;
        }
        else {
            return false;
        }
    }

    public static long metaDataToSampleLevelDB(String fileName, int featureSize, int catSize) throws IOException ,ClassNotFoundException{
        /**
         *@Description: 从文件中读数据，然后生成sampleList，这个函数文件的格式是先离散特征，再连续特征
         *@Param: [fileName, featureSize, catSize]
         *@return: ParaStructure.Sample.SampleList
         *@Author: SongZhen
         *@date: 下午10:26 18-11-13
         */
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String readline = null;
        br.readLine();


        long cat_index = 0;
        int countSampleListSize = 0;
        DB db=WorkerContext.kvStoreForLevelDB.getDb();

        CurrentTimeUtil.setStartTime();

        while ((readline = br.readLine()) != null && countSampleListSize < WorkerContext.sampleListSize) {
            String[] lineSplit = readline.split(",");  //在调试的时候，由于这个在下文没有调用，所有就没有给空间存储，其实就相当于废代码不编译
            float[] feature = new float[featureSize];
            long[] cat = new long[catSize];
            float click = Float.parseFloat(lineSplit[1]);

            // 给cat初始值全为-1，来表示上来所有cat属性都为missing value
            for (int i = 0; i < catSize; i++) {
                cat[i] = -1;
            }


            cat_index = getCat_index(Context.isDist,catSize, cat_index, db, lineSplit, cat);

            for (int i = 2 + catSize; i < 2 + catSize + featureSize; i++) {
                if (lineSplit[i].equals("")) {
                    feature[i - 2 - catSize] = 0f;
                } else {
                    feature[i - 2 - catSize] = Float.parseFloat(lineSplit[i]);
                }

            }

            Sample sample = new Sample(feature, cat, click);
            WorkerContext.kvStoreForLevelDB.getDb().put(("sample"+countSampleListSize).getBytes(),TypeExchangeUtil.toByteArray(sample));
            countSampleListSize++;
        }

        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("MetaDataToSample:");
        return cat_index;


    }


    public static long metaDataToSampleBatchLevelDB(String fileName, int featureSize, int catSize) throws IOException ,ClassNotFoundException{
        /**
         *@Description:
         *@Param: [fileName, featureSize, catSize]
         *@return: ParaStructure.Sample.SampleList
         *@Author: SongZhen
         *@date: 下午10:26 18-11-13
         */
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String readline = null;
        br.readLine();
        SampleList sampleBatch=new SampleList();


        long cat_index = 0;
        int countSampleListSize = 0;
        DB db=WorkerContext.kvStoreForLevelDB.getDb();

        CurrentTimeUtil.setStartTime();

        while ((readline = br.readLine()) != null && countSampleListSize < WorkerContext.sampleListSize) {
            String[] lineSplit = readline.split(",");  //在调试的时候，由于这个在下文没有调用，所有就没有给空间存储，其实就相当于废代码不编译
            float[] feature = new float[featureSize];
            long[] cat = new long[catSize];
            float click = Float.parseFloat(lineSplit[1]);


            // 给cat初始值全为-1，来表示上来所有cat属性都为missing value
            for (int i = 0; i < catSize; i++) {
                cat[i] = -1;
            }

            // 获取cat的index
            cat_index = getCat_index(Context.isDist,catSize, cat_index, db, lineSplit, cat);
            for (int i = 2 + catSize; i < 2 + catSize + featureSize; i++) {
                if (lineSplit[i].equals("")) {
                    feature[i - 2 - catSize] = 0f;
                } else {
                    feature[i - 2 - catSize] = Float.parseFloat(lineSplit[i]);
                }
            }


            Sample sample = new Sample(feature, cat, click);
            if(sampleBatch.sampleList.size()!=WorkerContext.sampleBatchSize){
                sampleBatch.sampleList.add(sample);

            }else {
                WorkerContext.kvStoreForLevelDB.getDb().put(("sampleBatch"+countSampleListSize/WorkerContext.sampleBatchSize).getBytes(),TypeExchangeUtil.toByteArray(sampleBatch));
                sampleBatch.sampleList.clear();
                sampleBatch.sampleList.add(sample);

            }
            countSampleListSize++;

        }

        if(sampleBatch.sampleList!=null){
            WorkerContext.kvStoreForLevelDB.getDb().put(("sampleBatch"+countSampleListSize/WorkerContext.sampleBatchSize).getBytes(),TypeExchangeUtil.toByteArray(sampleBatch));
        }



        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("MetaDataToSampleBatch:");
        return cat_index;


    }

    private static long getCat_index(boolean isDist,int catSize, long cat_index, DB db, String[] lineSplit, long[] cat) throws IOException, ClassNotFoundException {
        if(!isDist){
            for (int i = 2; i < 2 + catSize; i++) {
                if (isCatEmpty(lineSplit[i])) {
                    cat[i - 2] = -1;
                } else {
                    byte[] bytes=db.get(("CatDimMap"+lineSplit[i]).getBytes());
                    if (bytes!=null) {
                        cat[i - 2] = (Long)TypeExchangeUtil.toObject(bytes);
                    } else {
                        cat[i - 2] = cat_index;
                        db.put(("CatDimMap"+lineSplit[i]).getBytes(), TypeExchangeUtil.toByteArray(cat_index));
                        cat_index++;
                    }
                }

            }
        }else {
            WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getCat_indexFromServer(catSize,lineSplit,cat);

        }

        return cat_index;
    }



    public static void getSampleBatchListByBatchIndex(String fileName, int featureSize, int catSize) throws IOException ,ClassNotFoundException{
        /**
         *@Description:
         *@Param: [fileName, featureSize, catSize]
         *@return: ParaStructure.Sample.SampleList
         *@Author: SongZhen
         *@date: 下午10:26 18-11-13
         */
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String readline = null;
        br.readLine();
        SampleList sampleBatch=new SampleList();
        List<String[]> catList=new ArrayList<String[]>();
        Set<String> catSet=new HashSet<String>();



        int countSampleListSize = 0;
        DB db=WorkerContext.kvStoreForLevelDB.getDb();

        CurrentTimeUtil.setStartTime();

        // countSampleListSize就是当前已经读取的数据个数
        while ((readline = br.readLine()) != null && countSampleListSize <= WorkerContext.sampleListSize) {
            String[] lineSplit = new String[Context.featureSize+WorkerContext.catSize+2];
            String[] split=readline.split(",");
            for(int i=0;i<lineSplit.length;i++) {
                if(i<split.length){
                    lineSplit[i]=split[i];
                }else {
                    lineSplit[i]="-1";
                }
            }

            float[] feature = new float[featureSize];
            long[] cat = new long[catSize];
            float click = Float.parseFloat(lineSplit[1]);


            // 给cat初始值全为-1，来表示上来所有cat属性都为missing value
            for (int i = 0; i < catSize; i++) {
                cat[i] = -1;
            }




            for (int i = 2 + catSize; i < 2 + catSize + featureSize; i++) {
//                System.out.println(lineSplit.length);
//                System.out.println(i);
//                System.out.println(readline);
                if (lineSplit[i].equals("")) {
                    feature[i - 2 - catSize] = 0f;
                } else {
                    feature[i - 2 - catSize] = Float.parseFloat(lineSplit[i]);
                }
            }


            Sample sample = new Sample(feature, cat, click);
            if(sampleBatch.sampleList.size()!=WorkerContext.sampleBatchSize){
                catList.add(getMetaCat(lineSplit,catSet));
                sampleBatch.sampleList.add(sample);

            }else {
                // 或者Map
                Map<String,Long> dimMap=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getCatDimMapBySet(catSet);
                for(int i=0;i<catList.size();i++){
                    for(int j=0;j<catList.get(i).length;j++){
                        sampleBatch.sampleList.get(i).cat[j]=dimMap.get(catList.get(i)[j]);
                    }
                }

                WorkerContext.kvStoreForLevelDB.getDb().put(("sampleBatch"+(countSampleListSize/WorkerContext.sampleBatchSize-1)).getBytes(),TypeExchangeUtil.toByteArray(sampleBatch));
                catList.clear();
                sampleBatch.sampleList.clear();
                sampleBatch.sampleList.add(sample);

            }
            countSampleListSize++;

        }

        if(sampleBatch.sampleList!=null){
            WorkerContext.kvStoreForLevelDB.getDb().put(("sampleBatch"+(countSampleListSize/WorkerContext.sampleBatchSize)).getBytes(),TypeExchangeUtil.toByteArray(sampleBatch));
        }



        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("MetaDataToSampleBatch:");


    }

    public static String[] getMetaCat(String[] str,Set<String> catSet){
        String[] cat=new String[WorkerContext.catSize];
        for (int i=0;i<cat.length;i++){
            cat[i]=str[i+2];
            catSet.add(str[i+2]);
        }
        return cat;
    }


}
