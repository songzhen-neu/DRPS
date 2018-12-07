package context;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description: 全局常亮存储在这个类中
 * @author: SongZhen
 * @create: 2018-12-02 18:19
 */
public class Context {
    /** 网络通信server的相关配置*/
    public static Map<String,String> serverAddress=new HashMap<String, String>();

    /** 判断是否是服务器*/
    public static boolean isPServer;
    public static String currentServerIp;
    public static String currentServerPort;


    /** worker和server数量*/
    public static int workerNum;
    public static int serverNum;

    /** 判断是否已经初始化过了*/
    public static boolean inited=false;

    /** 判断是分布式执行还是单机执行*/
    public static enum Mode{
        STANDALONE, DISTRIBUTED
    }
    public static Mode mode;

    /** 判断是不是server的master机器，管参数分配的*/
    public static boolean isMaster;


    /** 判断是否异步*/
    public static boolean isAsy=false;

    /** 当前是几号机器*/
    public static int workerId;


    public static void init(){
        if (inited){
            return;
        }
        inited=true;

        serverAddress.put("firstServerIp","202.199.6.30");
        serverAddress.put("firstServerPort","8999");
        serverAddress.put("secondServerIp","172.20.10.3");
        serverAddress.put("secondServerPort","8999");
        serverAddress.put("thirdServerIp","172.20.10.13");
        serverAddress.put("thirdServerPort","8999");

        isPServer=true;
        isMaster=true;
        currentServerIp="firstServerIp";
        currentServerPort="firstServerPort";

        workerNum=3;
        serverNum=3;

        workerId=1;

        mode=Mode.DISTRIBUTED;
    }
}