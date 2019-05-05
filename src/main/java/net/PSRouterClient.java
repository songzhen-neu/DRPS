package net;

import context.Context;
import context.WorkerContext;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
        localhostPSWorker=new PSWorker("localhost",Context.serverPort.get(WorkerContext.workerId));
    }

    public void shutdownAll()throws InterruptedException{
        for(PSWorker psWorker:psWorkers){
            psWorker.shutdown();
        }
        localhostPSWorker.shutdown();
    }


    public void sendGradientMap(Map<String,Float> map){
        Map[] maps=divideMapByRouter(map);
        for(int i=0;i<maps.length;i++){
            psWorkers.get(i).sendGradientMap(maps[i]);
        }
        System.out.println("maps");
    }

    public Map[] divideMapByRouter(Map<String,Float> map){
        Map[] maps=new Map[Context.workerNum];
        for(int i=0;i<maps.length;i++){
            maps[i]=new HashMap<String,Float>();
        }
        for(String index:map.keySet()){
            if(index.contains("featParam")){
                maps[Context.masterId].put(index,map.get(index));

            }else {
                String[] indexSplit=index.split("m");
                Set<Long>[] VSet=(Set<Long>[]) WorkerContext.kvStoreForLevelDB.getVSet();
                boolean isInVSet=false;
                for(int i=0;i<VSet.length;i++){
                    if(VSet[i].contains(Long.parseLong(indexSplit[1]))){
                        maps[i].put(index,map.get(index));
                        isInVSet=true;
                    }
                }
                if(!isInVSet){
                    maps[Integer.parseInt(indexSplit[1])%Context.serverNum].put(index,map.get(index));
                }
                isInVSet=false;
            }

        }

        return maps;
    }
}