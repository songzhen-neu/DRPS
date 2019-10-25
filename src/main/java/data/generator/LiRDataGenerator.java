package data.generator;

import Util.RandomUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-09-29 14:16
 */
public class LiRDataGenerator {
    public static void main(String[] agrs) throws IOException {
        // 计算总的概率，有10亿个维度
        double p_total=0;
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("data/LoRData/skewed0.csv")));
        float[] label={0f,1f};


        // 假设100万个数据，每条数据
        for(int i=0;i<1000002;i++){
            String str=RandomUtil.random(label)+"";
            Set<Integer> set=new HashSet<Integer>();
            if(i%100000==0){
                System.out.println(i);
            }
            for(int j=0;j<8;j++){
                int k=-1;
                while (true){
                    if(!set.contains(k=RandomUtil.getIntRandomFromZeroToN((1500)))){
                        str=str+","+k+":"+RandomUtil.getRandomValue(0,1);
                        set.add(k);
                        break;
                    }
                }
            }
            for(int j=0;j<2;j++){
                int k=-1;
                while (true){
                    if(!set.contains(k=RandomUtil.getIntRandomFromZeroToN((1000000)))){
                        str=str+","+k+":"+RandomUtil.getRandomValue(0,1);
                        set.add(k);
                        break;
                    }
                }
            }

            str+="\n";
            bw.write(str);

        }
        bw.close();

    }

    public static boolean probability(double p){
        if(RandomUtil.getRandomValue(0,1)<p){
            return true;
        }else {
            return  false;
        }
    }
}