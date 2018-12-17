package Util;

import java.util.Random;

public class RandomUtil {

    public static float getRandomValue(float minValue,float maxValue){
        Random random=new Random();
        /*最小的值加上(0,1)*distance，就相当于随机了一个minValue到maxValue之间的一个最小值*/
        return random.nextInt(10000)/10000.0f*(maxValue-minValue)+minValue;
    }

    public static int getRandomZeroOrOne(){
        Random random=new Random();
        return random.nextInt(2);
    }
}
