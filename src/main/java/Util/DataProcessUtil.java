package Util;


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


    public static long metaToDB(String fileName, int featureSize, int catSize) throws IOException,ClassNotFoundException {
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
        if (Context.isCatForwardFeature) {
//            sparseDimSize=metaDataToSampleLevelDB(fileName, featureSize, catSize);
//            sparseDimSize=metaDataToBatchSampleLevelDB(fileName,featureSize,catSize);
            getBatchSampleListByBatchIndex(fileName,featureSize,catSize);

        } else if (!Context.isCatForwardFeature) {
            // 下面是连续特征在前，离散特征在后的代码
           System.out.println("方法还没写");
        }

        return sparseDimSize;

    }


    public static SampleList linerNormalization(SampleList sampleList){  // 这里标准化是为了提升训练精度。那么也就是(a-amin)/(amax-amin)，一定落在(0,1)区间内
        /*首先应该获取每一个属性栏最大和最小的参数，这里应该只对feature属性来做，因为cat属性只表示这意味出不出现，不表示具体值
         * 规范化这里究竟是否要真的使用max是有争议的，因为，可能噪声导致了部分数据值异常大，而影响了整体精度
         *
         *
         * */
        int featureSize=sampleList.featureSize;
        float[] max=new float[featureSize];
        float[] min=new float[featureSize];
        float[] dis=new float[featureSize];


        for(int i=0;i<featureSize;i++){
            max[i]= Float.MIN_VALUE;
            min[i]=Float.MAX_VALUE;
        }


        for(int i=0;i<sampleList.sampleList.size();i++){
            for(int j=0;j<featureSize;j++){
                float[] feature=sampleList.sampleList.get(i).feature;
                if(feature[j]>max[j]){
                    max[j]=feature[j];

                }
                if(feature[j]<min[j]){
                    min[j]=feature[j];
                }
            }
//            System.out.println(sampleList.sampleList.get(i).feature[1]);
        }

        for(int i=0;i<featureSize;i++){
            dis[i]=max[i]-min[i];
        }

        for(int i=0;i<sampleList.sampleList.size();i++){
            for(int j=0;j<featureSize;j++){
                float[] feature=sampleList.sampleList.get(i).feature;
                feature[j]=(feature[j]-min[j])/dis[j];
            }
        }

        return sampleList;
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
        DB db=Context.kvStoreForLevelDB.getDb();

        CurrentTimeUtil.setStartTime();

        while ((readline = br.readLine()) != null && countSampleListSize < Context.sampleListSize) {
            String[] lineSplit = readline.split(",");  //在调试的时候，由于这个在下文没有调用，所有就没有给空间存储，其实就相当于废代码不编译
            float[] feature = new float[featureSize];
            long[] cat = new long[catSize];
            boolean click = Boolean.parseBoolean(lineSplit[1]);

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
            Context.kvStoreForLevelDB.getDb().put(("sample"+countSampleListSize).getBytes(),TypeExchangeUtil.toByteArray(sample));
            countSampleListSize++;
        }

        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("MetaDataToSample:");
        return cat_index;


    }


    public static long metaDataToBatchSampleLevelDB(String fileName, int featureSize, int catSize) throws IOException ,ClassNotFoundException{
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
        SampleList batchSample=new SampleList();


        long cat_index = 0;
        int countSampleListSize = 0;
        DB db=Context.kvStoreForLevelDB.getDb();

        CurrentTimeUtil.setStartTime();

        while ((readline = br.readLine()) != null && countSampleListSize < Context.sampleListSize) {
            String[] lineSplit = readline.split(",");  //在调试的时候，由于这个在下文没有调用，所有就没有给空间存储，其实就相当于废代码不编译
            float[] feature = new float[featureSize];
            long[] cat = new long[catSize];
            boolean click = Boolean.parseBoolean(lineSplit[1]);


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
            if(batchSample.sampleList.size()!=Context.sampleBatchSize){
                batchSample.sampleList.add(sample);

            }else {
                Context.kvStoreForLevelDB.getDb().put(("batchSample"+countSampleListSize/Context.sampleBatchSize).getBytes(),TypeExchangeUtil.toByteArray(batchSample));
                batchSample.sampleList.clear();
                batchSample.sampleList.add(sample);

            }
            countSampleListSize++;

        }

        if(batchSample.sampleList!=null){
            Context.kvStoreForLevelDB.getDb().put(("batchSample"+countSampleListSize/Context.sampleBatchSize).getBytes(),TypeExchangeUtil.toByteArray(batchSample));
        }



        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("MetaDataTobatchSample:");
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
            WorkerContext.psWorker.getCat_indexFromServer(catSize,lineSplit,cat);

        }

        return cat_index;
    }



    public static long getBatchSampleListByBatchIndex(String fileName, int featureSize, int catSize) throws IOException ,ClassNotFoundException{
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
        SampleList batchSample=new SampleList();
        List<String[]> catList=new ArrayList<String[]>();
        Set<String> catSet=new HashSet<String>();


        long cat_index = 0;
        int countSampleListSize = 0;
        DB db=Context.kvStoreForLevelDB.getDb();

        CurrentTimeUtil.setStartTime();

        while ((readline = br.readLine()) != null && countSampleListSize < Context.sampleListSize) {
            String[] lineSplit = readline.split(",");  //在调试的时候，由于这个在下文没有调用，所有就没有给空间存储，其实就相当于废代码不编译
            float[] feature = new float[featureSize];
            long[] cat = new long[catSize];
            boolean click = Boolean.parseBoolean(lineSplit[1]);


            // 给cat初始值全为-1，来表示上来所有cat属性都为missing value
            for (int i = 0; i < catSize; i++) {
                cat[i] = -1;
            }




            for (int i = 2 + catSize; i < 2 + catSize + featureSize; i++) {
                if (lineSplit[i].equals("")) {
                    feature[i - 2 - catSize] = 0f;
                } else {
                    feature[i - 2 - catSize] = Float.parseFloat(lineSplit[i]);
                }
            }


            Sample sample = new Sample(feature, cat, click);
            if(batchSample.sampleList.size()!=Context.sampleBatchSize){
                catList.add(getMetaCat(lineSplit,catSet));
                batchSample.sampleList.add(sample);

            }else {
                // 或者Map
                Map<String,Long> dimMap=WorkerContext.psWorker.getCatDimMapBySet(catSet);
                for(int i=0;i<catList.size();i++){
                    for(int j=0;j<catList.get(i).length;j++){
                        batchSample.sampleList.get(i).cat[j]=dimMap.get(catList.get(i)[j]);
                    }
                }

                Context.kvStoreForLevelDB.getDb().put(("batchSample"+countSampleListSize/Context.sampleBatchSize).getBytes(),TypeExchangeUtil.toByteArray(batchSample));
                catList.clear();
                batchSample.sampleList.clear();
                batchSample.sampleList.add(sample);

            }
            countSampleListSize++;

        }

        if(batchSample.sampleList!=null){
            Context.kvStoreForLevelDB.getDb().put(("batchSample"+countSampleListSize/Context.sampleBatchSize).getBytes(),TypeExchangeUtil.toByteArray(batchSample));
        }



        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("MetaDataTobatchSample:");
        return cat_index;


    }

    public static String[] getMetaCat(String[] str,Set<String> catSet){
        String[] cat=new String[Context.catSize];
        for (int i=0;i<cat.length;i++){
            cat[i]=str[i+2];
            catSet.add(str[i+2]);
        }
        return cat;
    }


}
