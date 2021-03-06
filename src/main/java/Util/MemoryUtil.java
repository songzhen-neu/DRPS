package Util;

/**
 * @program: CtrForBigModel
 * @description: 用于获得内存的相关参数
 * @author: SongZhen
 * @create: 2018-11-12 16:50
 */
public class MemoryUtil {
    private static long startMemory;
    private static long endMemory;
    private static long getFreeMemory(){
        /**
        *@Description: 获取当前空闲内存大小,单位是MB
        *@Param: []
        *@return: long
        *@Author: SongZhen
        *@date: 18-11-12
        */
        Runtime rt=Runtime.getRuntime();
        return rt.freeMemory()/1024/1024;

    }

    public static void setStartMemory(){
        startMemory=getFreeMemory();
    }

    public static void setEndMemory(){
        endMemory=getFreeMemory();
    }

    public static void showUsedMemory(String flag){
        System.out.println(flag+":"+(startMemory-endMemory)+"MB");
    }

    public static void showFreeMemory(String flag){
        System.out.println(flag+":"+getFreeMemory()+"MB");
    }

    public static void releaseMemory(){
        System.gc();
    }

}