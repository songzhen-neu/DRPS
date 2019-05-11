package TestClass;

import com.google.common.util.concurrent.RateLimiter;
import context.Context;
import context.WorkerContext;
import net.BMessage;
import net.IMessage;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-04-08 20:25
 */
public class TestGrpc {
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException, TimeoutException {
        Context.init();
        WorkerContext.init();
        // 阻塞当前主线程，执行完收到返回结果，才继续执行
        for (int i = 0; i < 10; i++) {
            IMessage iMessage = WorkerContext.psRouterClient.getPsWorkers().get(0).getBlockingStub().testGrpc(IMessage.newBuilder().setI(i).build());
            System.out.println(iMessage.getI());
        }

        {
            // 不阻塞当前主线程，可以继续执行，但是使用get函数的时候阻塞
            Future<IMessage> iMessage = WorkerContext.psRouterClient.getPsWorkers().get(0).getFutureStub().testGrpc(IMessage.newBuilder().setI(-1).build());
            System.out.println("-1" + iMessage.isDone());
            Future<IMessage> iMessage1 = WorkerContext.psRouterClient.getPsWorkers().get(0).getFutureStub().testGrpc(IMessage.newBuilder().setI(1).build());
            System.out.println("1");
            Thread.sleep(1000);
            System.out.println(iMessage.isDone());
        }


        RateLimiter rateLimiter=RateLimiter.create(0.5);







    }
}