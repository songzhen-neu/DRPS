package Util;

import java.util.Random;

public class RandomUtil {
    private static Random random=new Random();
    public static float getRandomValue(float minValue,float maxValue){

        /*最小的值加上(0,1)*distance，就相当于随机了一个minValue到maxValue之间的一个最小值*/
        return random.nextInt(10000)/10000.0f*(maxValue-minValue)+minValue;
    }

    // 在指定数中产生随机数
    public static float random(float[] index){
        return index[random.nextInt(index.length)];
    }



    public static int getIntRandomFromZeroToN(int n){
        /**
        *@Description: 这里取的是[0,n)之间的整数
        *@Param: [n]
        *@return: int
        *@Author: SongZhen
        *@date: 上午8:53 19-1-21
        */
        return random.nextInt(n);
    }
}
