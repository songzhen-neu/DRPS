
import context.Context;
import net.PSWorker;
import net.PServer;
import org.jblas.FloatMatrix;

import java.io.IOException;


/**
 * @program: simplePsForModelPartition
 * @description: 用参数服务器解决模型划分问题
 * @author: SongZhen
 * @create: 2018-12-02 15:49
 */
public class PsForModelPartition {
    public static void main(String args[])throws IOException,InterruptedException {
        if(Context.isPServer){
            // 当前server的端口号
            int port=Integer.parseInt(Context.serverAddress.get(Context.currentServerPort));
            PServer pServer=new PServer(port);
            pServer.start();
            pServer.blockUntilShutdown();
        }else {
            PSWorker worker=new PSWorker(Context.serverAddress.get("firstServerIp"),Integer.parseInt(Context.serverAddress.get("firstServerPort")));
            worker.push();
        }


    }




}