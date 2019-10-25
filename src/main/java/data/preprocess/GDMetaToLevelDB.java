package data.preprocess;

import Util.MemoryUtil;
import Util.TypeExchangeUtil;
import context.Context;
import context.QuickSetting;
import context.ServerContext;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import org.iq80.leveldb.DB;

import java.io.*;
import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-09-29 19:45
 */
public class GDMetaToLevelDB {
    // generated data GD
    // 获取数据
    public static void main(String[] args) throws IOException {
        Context.init();
        WorkerContext.init();


        BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(new File(WorkerContext.myDataPath))));
        DB db = WorkerContext.kvStoreForLevelDB.getDb();
        String readline = null;
        int countSampleListSize = 0;
        SampleList sampleList=new SampleList();
        int batchNumCounter=0;

        while ((readline = bf.readLine()) != null && countSampleListSize < WorkerContext.sampleListSize) {
            // 这里+2，是因为前面有id和isClick属性
            if(countSampleListSize%1000000==0){
                System.out.println(countSampleListSize);
            }
            String[] split = readline.split(",");
            float click=-1;
            long[] cat=new long[split.length-1];
            float[] catValue=new float[split.length-1];

            for(int i=0;i<split.length;i++) {
                if(i==0) {
                    click=Float.parseFloat(split[0]);
                }else {
                    String[] innerSplit=split[i].split(":");
                    cat[i-1]=Integer.parseInt(innerSplit[0]);
                    catValue[i-1]=Float.parseFloat(innerSplit[1]);
                }
            }

            Sample sample=new Sample(click,cat,catValue);
            sampleList.sampleList.add(sample);
            countSampleListSize++;

            if(sampleList.sampleList.size()%WorkerContext.sampleBatchSize==0){
                db.put(("sampleBatch"+batchNumCounter).getBytes(),TypeExchangeUtil.toByteArray(sampleList));
                batchNumCounter++;
                sampleList.sampleList.clear();
            }

    }

    if(readline==null&&sampleList.sampleList.size()!=0){
        db.put(("sampleBatch"+batchNumCounter).getBytes(),TypeExchangeUtil.toByteArray(sampleList));
        batchNumCounter++;
    }
    bf.close();
        db.close();


}

}