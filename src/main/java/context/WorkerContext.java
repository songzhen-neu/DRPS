package context;

import net.PSWorker;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-10 18:46
 */
public class WorkerContext {
    public static PSWorker psWorker;
    public static String masterIp;
    public static String masterPort;
    public static int workerId;

    public static void init(){
        masterIp="localhost";
        masterPort="8999";
        workerId=1;

        psWorker=new PSWorker();
        psWorker.setChannel(masterIp,Integer.parseInt(masterPort));
    }
}