package net;

import context.Context;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: simplePsForModelPartition
 * @description: 用来路由哪些消息往哪发
 * @author: SongZhen
 * @create: 2018-12-17 22:29
 */

@Data
public class PSRouterClient{
    static Logger logger = LoggerFactory.getLogger(PSRouterClient.class);
    List<PSWorker> psWorkers=new ArrayList<PSWorker>();
    PSWorker localhostPSWorker;

    public PSRouterClient(){
        for(int i=0;i<Context.workerNum;i++){
            psWorkers.add(new PSWorker(Context.serverIp.get(i),Context.serverPort.get(i)));
        }
        localhostPSWorker=new PSWorker("localhost",Context.serverPort.get(Context.masterId));
    }

    public void shutdownAll()throws InterruptedException{
        for(PSWorker psWorker:psWorkers){
            psWorker.shutdown();
        }
        localhostPSWorker.shutdown();
    }
}