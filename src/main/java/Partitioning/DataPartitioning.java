package Partitioning;


import com.sun.corba.se.spi.orbutil.threadpool.Work;
import context.Context;
import context.WorkerContext;
import dataStructure.dataPartitioning.PartitionSetting;
import dataStructure.partition.Partition;
import io.grpc.internal.ReadableBuffer;

import java.io.*;

/**
 * @program: simplePsForModelPartition
 * @description: 进行数据划分
 * @author: SongZhen
 * @create: 2018-12-10 10:26
 */
public class DataPartitioning{

    public static void dataPartitioning(PartitionSetting setting ) throws IOException{
        Context.init();
        File file=new File(setting.dataPath+".csv");
        BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String head=br.readLine();
        long readDataNum=0;
        String str;

        for(int i=0;i<setting.dataPartitionNum;i++){
            File writeFile=new File(setting.dataPath+(i)+".csv");
            if(!writeFile.exists()){
                writeFile.createNewFile();
            }
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile)));
            bw.write(head);
            while((str=br.readLine())!=null&&readDataNum<setting.partitionedDataSize){
                bw.write(str+"\n");
                readDataNum++;
            }
            readDataNum=0;
            bw.close();
        }

        br.close();


    }


}