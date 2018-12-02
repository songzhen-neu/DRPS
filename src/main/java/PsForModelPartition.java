
import Gradient.GradientStructure;
import context.Context;
import net.PSWorker;
import net.PServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description: 用参数服务器解决模型划分问题
 * @author: SongZhen
 * @create: 2018-12-02 15:49
 */
public class PsForModelPartition {
    public static void main(String args[])throws IOException,InterruptedException {
        if(Context.isServer){
            PServer pServer=new PServer(Context.port,Context.workNum);
            pServer.start();
            pServer.blockUntilShutdown();
        }else {
//            PSWorker worker=new PSWorker("202.199.6.30",Context.port);
//            String user="worker1";
//            worker.greet(user);
//            worker.shutdown();
            PSWorker worker=new PSWorker("202.199.6.30",Context.port);


        }


    }




}