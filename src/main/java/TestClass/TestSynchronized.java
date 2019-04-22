package TestClass;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-04-09 22:23
 */
public class TestSynchronized {
    public static void main(String[] args) throws InterruptedException {
        final int totalThread = 10;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(totalThread);
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < totalThread; i++) {
            executorService.execute(() -> {
                System.out.print("before..");
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
                System.out.print("after..");
            });
        }

        while(cyclicBarrier.getNumberWaiting()>0){
            System.out.println(cyclicBarrier.getNumberWaiting());
            Thread.sleep(1);
        }
        cyclicBarrier.reset();



        executorService.shutdown();


        // 测试锁的相关内容
        testEmbeddingLock();
    }

    public static void testEmbeddingLock(){
        Integer[] a=new Integer[5];
        int totalThread=5;
        ExecutorService executorService=Executors.newCachedThreadPool();
        for(int i=0;i<a.length;i++){
            a[i]=new Integer(i);
        }

        for(int i=0;i<totalThread;i++){
            int finalI = i;
            executorService.execute(()->{
            synchronized (a){
                synchronized (a[finalI]){
                    try {
                        if(finalI==2){
                            Thread.sleep(1000);
                        }
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    System.out.println(finalI +":"+a[0]);
                }

            }


            });
        }
        executorService.shutdown();
    }
}