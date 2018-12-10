
import context.Context;
import net.PSWorker;
import net.PServer;

import java.io.IOException;


/**
 * @program: simplePsForModelPartition
 * @description: 用参数服务器解决模型划分问题
 * @author: SongZhen
 * @create: 2018-12-02 15:49
 */
public class PsForModelPartitionServer {
    public static void main(String args[]) throws IOException, InterruptedException,ClassNotFoundException {
        Context.init();

        // 当前server的端口号
        int port = Integer.parseInt(Context.serverAddress.get(Context.currentServerPort));
        PServer pServer = new PServer(port);
        pServer.start();
        pServer.blockUntilShutdown();


    }




}