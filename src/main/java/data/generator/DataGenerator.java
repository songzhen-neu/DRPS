package data.generator;

import Util.RandomUtil;
import javafx.scene.control.RadioMenuItem;

import java.io.*;
import java.util.*;

/**
 * @program: simplePsForModelPartition
 * @description: 用于生成和合成数据集
 * @author: SongZhen
 * @create: 2019-09-16 16:30
 */
public class DataGenerator {
    public static void main(String[] args) throws IOException {
        buildDataByUserOrder();
        // test 数据条数
//        partitionToLittleFile();

//            getFileDataRowNum();
    }

    public static void getFileDataRowNum() throws IOException{
        BufferedReader bw0 = new BufferedReader(new InputStreamReader(new FileInputStream("data/LMFData/ratings0.csv")));
        long i=0l;
        while (bw0.readLine()!=null){
            i++;
        }
        // 数据条数7494138106
        System.out.println(i);
        bw0.close();
    }

    // 打乱csv行顺序
    public static void disruptedData() throws IOException{
        // 分成小文件


    }

    public static void partitionToLittleFile() throws IOException{



    }

    // 按照用户顺序构建数据集
    public static void buildDataByUserOrder() throws IOException{
        Set<Integer> set = new HashSet<Integer>();
        BufferedWriter bw0 = new BufferedWriter(new FileWriter(new File("data/LMFData/matrixData0.csv")));
//        BufferedWriter bw1 = new BufferedWriter(new FileWriter(new File("matrixData1.csv")));
//        BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File("matrixData2.csv")));
//        BufferedWriter bw3 = new BufferedWriter(new FileWriter(new File("matrixData3.csv")));
//        BufferedWriter bw4 = new BufferedWriter(new FileWriter(new File("matrixData4.csv")));
//        BufferedWriter bw5 = new BufferedWriter(new FileWriter(new File("matrixData5.csv")));
//        BufferedWriter bw6 = new BufferedWriter(new FileWriter(new File("matrixData6.csv")));
//        BufferedWriter bw7 = new BufferedWriter(new FileWriter(new File("matrixData7.csv")));
//        BufferedWriter bw8 = new BufferedWriter(new FileWriter(new File("matrixData8.csv")));
//        BufferedWriter bw9 = new BufferedWriter(new FileWriter(new File("matrixData9.csv")));
//        BufferedWriter bw10 = new BufferedWriter(new FileWriter(new File("matrixData10.csv")));
//        BufferedWriter bw11 = new BufferedWriter(new FileWriter(new File("matrixData11.csv")));
//        BufferedWriter bw12 = new BufferedWriter(new FileWriter(new File("matrixData12.csv")));
//        BufferedWriter bw0 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("matrixData0.csv",true)));





        float[] index = {0f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 5f};
        for(int i=0;i<500000;i++){
            int user=RandomUtil.getIntRandomFromZeroToN(10000);
            int movie=RandomUtil.getIntRandomFromZeroToN(10000);
            String str=user+","+movie+","+RandomUtil.random(index)+"\n";
            bw0.write(str);
        }

        for(int i=0;i<500002;i++){
            int user=RandomUtil.getIntRandomFromZeroToN(1000000);
            int movie=RandomUtil.getIntRandomFromZeroToN(10000);
            String str=user+","+movie+","+RandomUtil.random(index)+"\n";
            bw0.write(str);
        }
        bw0.close();

    }


}