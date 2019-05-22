package Util;


import com.sun.corba.se.spi.orbutil.threadpool.Work;
import context.Context;
import context.WorkerContext;
import dataStructure.SparseMatrix.Matrix;
import dataStructure.SparseMatrix.MatrixElement;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import lombok.Synchronized;
import net.PSWorker;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.util.SizeOf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.Future;

/**
 * @program: CtrForBigModel
 * @description: 对训练集进行处理
 * @author: SongZhen
 * @create: 2018-11-12 16:38
 */
public class DataProcessUtil {


    static Logger logger=LoggerFactory.getLogger(DataProcessUtil.class.getName());


    public static void metaToDB(String fileName, int featureSize, int catSize) throws IOException,ClassNotFoundException {
        /**
         *@Description: 读取数据
         * 针对第一维是ID，第二维是标签，之后先是cat，然后是feature
         *@Param: [fileName, featureSize, catSize]
         *@return: ParaStructure.Sample.SampleList
         *@Author: SongZhen
         *@date: 18-11-12
         */



        // 以下是离散特征在前连续特征在后的代码
        if (WorkerContext.isCatForwardFeature) {

            getSampleBatchListByBatchIndexByMaster(fileName,featureSize,catSize);
//            getSampleBatchListByBatchIndex(fileName,featureSize,catSize);
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
        logger.info("calculate local max and min value of features ");
        for(int i=0;i<WorkerContext.sampleBatchListSize;i++){
            SampleList batch=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+i).getBytes()));
            for(int j=0;j<batch.sampleList.size();j++){
                float[] feature=batch.sampleList.get(j).feature;
                for(int k=0;k<feature.length;k++){
                    if(Math.abs(feature[k])>max[k]){
                        max[k]=Math.abs(feature[k]);
                    }
                    if(Math.abs(feature[k])<min[k]){
                        min[k]=Math.abs(feature[k]);
                    }
                }
            }

        }

        // 将最大和最小值发送给server，并获得全局最大最小的feature
        logger.info("sent local max and min features ");
        PSWorker psWorker=WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId);
        logger.info("get global Max Min Value of features");
        psWorker.getGlobalMaxMinOfFeature(max,min);


        // 遍历所有数据，对每一条数据的每个sample进行规范化
        logger.info("linearNormalization start");
        for(int i=0;i<WorkerContext.sampleBatchListSize;i++){
            SampleList batch=(SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch"+i).getBytes()));
            for(int j=0;j<batch.sampleList.size();j++){
                float[] feature=batch.sampleList.get(j).feature;
                for(int k=0;k<feature.length;k++){
                    if(max[k]==min[k]){
                        feature[k]=0;
                    }else {
                        feature[k]=(feature[k]-min[k])/(max[k]-min[k]);
                    }

                }
            }

            // 把sampleBatch写入数据库
            db.delete(("sampleBatch"+i).getBytes());
            db.put(("sampleBatch"+i).getBytes(),TypeExchangeUtil.toByteArray(batch));

        }
        logger.info("linearNormalization end");



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



    // 这个算法是基于多个server的，进行one-hot构建的，有点问题
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
        // 长度为server的大小，用来判定哪个参数传递给哪个server进行编码
        List<Set> catSetList=new ArrayList<Set>();


        int countSampleListSize = 0;
        DB db=WorkerContext.kvStoreForLevelDB.getDb();


        // 初始化catSetList，大小是server的大小
        for(int i=0;i<Context.serverNum;i++){
            catSetList.add(new HashSet<String>());
        }

        // countSampleListSize就是当前已经读取的数据个数
        while ((readline = br.readLine()) != null && countSampleListSize <= WorkerContext.sampleListSize) {
            // 这里+2，是因为前面有id和isClick属性
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
//                getMetaCat(catSetList,lineSplit,catSet);
                catList.add(getMetaCat(lineSplit,catSetList));
                sampleBatch.sampleList.add(sample);

            }else {

                Map<String,Long> dimMaps=new HashMap<String, Long>();
                for(int i=0;i<catSetList.size();i++){
                    CurrentTimeUtil.setStartTime();

                    // 这块多个worker的数据集同时访问多个服务器，那么这里的CurIndexNum极大可能是乱的，导致了总的维度的减少
                    Map<String,Long> dimMap=WorkerContext.psRouterClient.getPsWorkers().get(i).getCatDimMapBySet(catSetList.get(i));


                    // 获取并向其他机器发送当前的index个数
                    for(int j=0;j<Context.serverNum;j++){
                        if(j!=i){
                            // 这些是并行的，那么就容易出问题
                            WorkerContext.psRouterClient.getPsWorkers().get(j).setCurIndexNum(dimMap.get("CurIndexNum"));
                        }
                    }


                    for(String key:dimMap.keySet()){
                        dimMaps.put(key,dimMap.get(key));
                    }

                }

                for(int i=0;i<catList.size();i++){
                    for(int j=0;j<catList.get(i).length;j++){

                        sampleBatch.sampleList.get(i).cat[j]=dimMaps.get(catList.get(i)[j]);

                    }
                }



                WorkerContext.kvStoreForLevelDB.getDb().put(("sampleBatch"+(countSampleListSize/WorkerContext.sampleBatchSize-1)).getBytes(),TypeExchangeUtil.toByteArray(sampleBatch));
                catList.clear();
                for(int i=0;i<Context.serverNum;i++){
                    catSetList.get(i).clear();
                }


                sampleBatch=new SampleList();
                MemoryUtil.releaseMemory();
                sampleBatch.sampleList.add(sample);
                catList.add(getMetaCat(lineSplit,catSetList));

            }
            countSampleListSize++;

        }

        if(sampleBatch.sampleList!=null){
            WorkerContext.kvStoreForLevelDB.getDb().put(("sampleBatch"+(countSampleListSize/WorkerContext.sampleBatchSize)).getBytes(),TypeExchangeUtil.toByteArray(sampleBatch));
        }



        MemoryUtil.showFreeMemory("before call gc");
        // 显示调用gc并不会强制释放内存，虚拟机会尽最大努力从所有丢弃的对象中回收空间
        MemoryUtil.releaseMemory();



        br.close();



    }

    public static void getSampleBatchListByBatchIndexByMaster(String fileName, int featureSize, int catSize) throws IOException, ClassNotFoundException {
        /**
         *@Description:这是只在master上
         *@Param: [fileName, featureSize, catSize]
         *@return: ParaStructure.Sample.SampleList
         *@Author: SongZhen
         *@date: 下午10:26 18-11-13
         */


        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String readline = null;
        br.readLine();
        SampleList sampleBatch = new SampleList();
        // 每个元素都是一个cat长度的String数组，表示这个batch的所有cat
        List<String[]> catList = new ArrayList<String[]>();

        Set<String> catSet = new HashSet<String>();
        int countSampleListSize = 0;
        DB db = WorkerContext.kvStoreForLevelDB.getDb();

        // countSampleListSize就是当前已经读取的数据个数
        while ((readline = br.readLine()) != null && countSampleListSize <=WorkerContext.sampleListSize) {
            // 这里+2，是因为前面有id和isClick属性
            String[] lineSplit = new String[Context.featureSize + WorkerContext.catSize + 2];
            String[] split = readline.split(",");
            for (int i = 0; i < lineSplit.length; i++) {
                if (i < split.length) {
                    lineSplit[i] = split[i];
                } else {
                    lineSplit[i] = "-1";
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
                if (lineSplit[i].equals("")||(Float.parseFloat(lineSplit[i])==Float.NaN)) {
                    feature[i - 2 - catSize] = 0f;
                } else {
                    feature[i - 2 - catSize] = Float.parseFloat(lineSplit[i]);
                }
            }


            Sample sample = new Sample(feature, cat, click);

            if (sampleBatch.sampleList.size() !=WorkerContext.sampleBatchSize) {
//                getMetaCat(catSetList,lineSplit,catSet);
                catList.add(getMetaCat_ForMasterBuild(lineSplit, catSet));
                sampleBatch.sampleList.add(sample);
                countSampleListSize++;
            } else {

                Map<String, Long> dimMap = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).getCatDimMapBySet(catSet);


                for (int i = 0; i < catList.size(); i++) {
                    for (int j = 0; j < catList.get(i).length; j++) {
                        sampleBatch.sampleList.get(i).cat[j] = dimMap.get(catList.get(i)[j]);
                    }
                }

                WorkerContext.kvStoreForLevelDB.getDb().put(("sampleBatch" + (countSampleListSize / WorkerContext.sampleBatchSize - 1)).getBytes(), TypeExchangeUtil.toByteArray(sampleBatch));
                catList.clear();
                catSet.clear();
//                sampleBatch = new SampleList();
                MemoryUtil.releaseMemory();
//                sampleBatch.sampleList.add(sample);
                catList.add(getMetaCat_ForMasterBuild(lineSplit, catSet));
                countSampleListSize++;
                sampleBatch.sampleList.clear();
            }

        }

        if (sampleBatch.sampleList != null) {
            WorkerContext.kvStoreForLevelDB.getDb().put(("sampleBatch" + ((countSampleListSize) / (WorkerContext.sampleBatchSize))).getBytes(), TypeExchangeUtil.toByteArray(sampleBatch));
        }


        MemoryUtil.showFreeMemory("before call gc");
        // 显示调用gc并不会强制释放内存，虚拟机会尽最大努力从所有丢弃的对象中回收空间
        MemoryUtil.releaseMemory();
        br.close();


    }

    public static String[] getMetaCat(String[] str,List<Set> catSetList){
        /**
        *@Description: 根据lineSplit来获取一个cat属性的String数组，
         * 并且按照hashcode分给不同的server，也就是划分在catSetList里。
        *@Param: [str, catSetList]
        *@return: java.lang.String[]
        *@Author: SongZhen
        *@date: 下午3:29 19-1-21
        */
        String[] cat=new String[WorkerContext.catSize];
        for (int i=0;i<cat.length;i++){
            cat[i]=str[i+2];
//            if(cat[i].equals("07d7df22")||cat[i].equals("a99f214a")||cat[i].equals("ecad2386")){
//                System.out.println("cat:"+cat[i]+",hashcode:"+(cat[i].hashCode())+",serverId:"+Math.abs(cat[i].hashCode())%3);
//            }
            int serverId=Math.abs(str[i+2].hashCode()%Context.serverNum);
            catSetList.get(serverId).add(str[i+2]);
        }
        return cat;
    }


    public static String[] getMetaCat_ForMasterBuild(String[] str,Set catSet){
        /**
         *@Description: 根据lineSplit来获取一个cat属性的String数组，
         * 并且按照hashcode分给不同的server，也就是划分在catSetList里。
         *@Param: [str, catSetList]
         *@return: java.lang.String[]
         *@Author: SongZhen
         *@date: 下午3:29 19-1-21
         */
        String[] cat=new String[WorkerContext.catSize];
        for (int i=0;i<cat.length;i++){
            cat[i]=str[i+2];
//            if(cat[i].equals("07d7df22")||cat[i].equals("a99f214a")||cat[i].equals("ecad2386")){
//                System.out.println("cat:"+cat[i]+",hashcode:"+(cat[i].hashCode())+",serverId:"+Math.abs(cat[i].hashCode())%3);
//            }

            catSet.add(str[i+2]);
        }
        return cat;
    }

    public static void printLs_partitionedVset(List<Set>[] lsArray){
        for(int i=0;i<lsArray.length;i++){
            System.out.println("ls["+i+"]:");
            for(int j=0;j<lsArray[i].size();j++){
                System.out.print("set["+j+"]:");
                for(Long l:(Set<Long>)lsArray[i].get(j)){
                    System.out.print(l+",");
                }
                System.out.println(" ");
            }
        }
    }

    public static void showVAccessNum(Map<Long,Integer> map){
        for(long l:map.keySet()){
            logger.info("l:"+l+",num:"+map.get(l));
        }
    }


    public static void metaToDB_LMF(){
        // 这里需要对矩阵分batch，然后存入到数据库中
        DB db=WorkerContext.kvStoreForLevelDB.getDb();
        int count=0;
        int batchCount=0;
        // 先读数据
        try {
            BufferedReader br=new BufferedReader(new FileReader(WorkerContext.myDataPath_LMF));
            String row;
            Matrix matrix=new Matrix();
            while(( row=br.readLine()) !=null&&count<=WorkerContext.sampleListSize_LMF){
                String[] data=row.split(",");
                // 0 存储的是user，1存储的是movie，2存储的是电影评分
                // 这里是要构建batch，可以直接构建成稀疏矩阵的形式（key-value）
                if(data[0].equals("id")){
                    continue;
                }
                MatrixElement matrixElement=new MatrixElement();
                matrixElement.set((Integer.parseInt(data[0])-1),(Integer.parseInt(data[1])-1),Float.parseFloat(data[2]));
                matrix.matrix.add(matrixElement);
                count++;
                if(count%WorkerContext.sampleBatchSize_LMF==0){
                    db.put(("batchLMF"+batchCount).getBytes(),TypeExchangeUtil.toByteArray(matrix));
                    batchCount++;
                    matrix.matrix.clear();
                }
            }

            // 样本不刚好是batchsize的整数倍
            if(matrix.matrix.size()!=0){
                db.put(("batchLMF"+batchCount).getBytes(),TypeExchangeUtil.toByteArray(matrix));
            }


        }catch (IOException e){
            e.printStackTrace();
        }




    }


}
