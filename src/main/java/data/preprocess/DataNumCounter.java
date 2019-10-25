package data.preprocess;

import dataStructure.dataPartitioning.PartitionSetting;

import java.io.*;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-10-01 16:18
 */
public class DataNumCounter {
    public static void main(String[] args) throws IOException {

        File file=new File("data/LoRData/train0.csv");
        // train.csv 40428968
        // skewed0.csv 1000000


        BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        int count=0;
        while(br.readLine()!=null){
            count++;
        }
        System.out.println(count);


    }
}