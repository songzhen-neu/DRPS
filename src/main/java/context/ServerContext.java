package context;

import Util.TypeExchangeUtil;
import store.KVStoreForLevelDB;

import java.io.IOException;
import java.util.List;
import java.util.Set;


/**
 * @program: simplePsForModelPartition
 * @description: 全局常亮存储在这个类中
 * @author: SongZhen
 * @create: 2018-12-02 18:19
 */
public class ServerContext {

    /** 当前是第几台server*/
    public static int serverId;

    /** 判断是否已经初始化过了*/
    private static boolean inited=false;

    /** 判断是否异步*/
    public static boolean isAsy;

    /** 磁盘上的k-v数据库*/
    public static KVStoreForLevelDB kvStoreForLevelDB=new KVStoreForLevelDB();
    public static String levelDBPathForServer;


    public static void init()  throws IOException{
        if (inited){
            return;
        }
        String algorithmName=null;
        serverId=QuickSetting.serverId;
        isAsy=false;
        switch (QuickSetting.algorithmName){
            case "LiR": algorithmName="LiR";break;
            case "LoR": algorithmName="LoR";break;
            case "SVM": algorithmName="SVM";break;
            case "LMF": algorithmName="LMF";break;
            default:System.out.println("无该算法");
        }


        levelDBPathForServer="data/leveldbForServer/";
        kvStoreForLevelDB.init(levelDBPathForServer,algorithmName);
        inited=true;









    }
}