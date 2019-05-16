package Util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-16 15:38
 */
public class DataSetStatistic {
    public static void main(String[] args) throws IOException {
        statisticForSVM();

    }



    public static void statisticForLMF() throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data/LMFData/Goodbooks.csv")));
        String readline;
        int max1=Integer.MIN_VALUE,min1=Integer.MAX_VALUE;
        int max2=Integer.MIN_VALUE,min2=Integer.MAX_VALUE;
        int count=0;
        while((readline=br.readLine())!=null){
            String[] rl_split=readline.split(",");
            if(Integer.parseInt(rl_split[0])>max1){
                max1=Integer.parseInt(rl_split[0]);
            }
            if(Integer.parseInt(rl_split[0])<min1){
                min1=Integer.parseInt(rl_split[0]);
            }
            if(Integer.parseInt(rl_split[1])>max2){
                max2=Integer.parseInt(rl_split[1]);
            }
            if(Integer.parseInt(rl_split[1])<min2){
                min2=Integer.parseInt(rl_split[1]);
            }
            count++;
        }

        System.out.println("max1:"+max1+",min1:"+min1+",max2:"+max2+",min2:"+min2);
        System.out.println("count:"+count);
    }

    public static void statisticForLoR() throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data/LoRData/train.csv")));
        String readline;
        br.readLine();
        int count=0;
        while((readline=br.readLine())!=null){
            count++;
        }

        System.out.println("count:"+count);
    }

    public static void statisticForLiR() throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data/LiRData/housing.csv")));
        String readline;
        br.readLine();
        int count=0;
        while((readline=br.readLine())!=null){
            count++;
        }

        System.out.println("count:"+count);
    }

    public static void statisticForSVM() throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data/SVMData/dataset.csv")));
        String readline;
        br.readLine();
        int count=0;
        while((readline=br.readLine())!=null){
            count++;
        }

        System.out.println("count:"+count);
    }
}