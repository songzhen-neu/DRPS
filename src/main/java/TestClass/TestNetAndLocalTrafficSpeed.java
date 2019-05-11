package TestClass;

import Util.CurrentTimeUtil;
import context.Context;
import context.WorkerContext;
import net.IListMessage;
import net.IMessage;
import net.PSWorker;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.IOException;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-10 12:40
 */
public class TestNetAndLocalTrafficSpeed {
    /**
    *@Description: 经过测试，在局域网大概100MB/S的传输速度的时候，传本地和传remote速度差不太多
     * 大概以下几组数据
     * remote：739
     * local：537
    *@Param:
    *@return:
    *@Author: SongZhen
    *@date: 下午1:49 19-5-10
    */
    public static void main(String[] args)throws IOException {
        Context.init();
        WorkerContext.init();
        int messageLength=20000000;
        IListMessage.Builder message= net.IListMessage.newBuilder();
        for(int i=0;i<messageLength;i++){
            message.addList(i);
        }


        PSWorker psWorker_remote=new PSWorker("192.168.111.102",9011);
        PSWorker psWorker_local=new PSWorker("localhost",9010);





        CurrentTimeUtil.setStartTime();
        psWorker_remote.getBlockingStub().sendIListMessage(message.build());
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("remote");

        CurrentTimeUtil.setStartTime();
        psWorker_local.getBlockingStub().sendIListMessage(message.build());
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("local");

        CurrentTimeUtil.setStartTime();
        psWorker_remote.getBlockingStub().sendIListMessage(message.build());
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("remote");

        CurrentTimeUtil.setStartTime();
        psWorker_local.getBlockingStub().sendIListMessage(message.build());
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("local");


    }
}