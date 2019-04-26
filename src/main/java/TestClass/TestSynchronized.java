package TestClass;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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

        while (cyclicBarrier.getNumberWaiting() > 0) {
            System.out.println(cyclicBarrier.getNumberWaiting());
            Thread.sleep(1);
        }
        cyclicBarrier.reset();


        executorService.shutdown();


        // 测试锁的相关内容
        testEmbeddingLock();

//        printAB();
    }

    public static void testEmbeddingLock() {
        Integer[] a = new Integer[5];
        Integer b=new Integer(0);
        int totalThread = 5;
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < a.length; i++) {
            a[i] = new Integer(i);
        }

        for (int i = 0; i < 2; i++) {

            int finalI = i;
            executorService.execute(() -> {
                if(finalI ==0){
                    synchronized (b) {
                        try {
                            synchronized (a){
                                System.out.println("before b.wait");
                                b.wait();
                                System.out.println("aa");
                                a.wait();
                                System.out.println("a");
                            }

                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }else {
                    try {
                        Thread.sleep(1000);
                        synchronized (b){
                            System.out.println("before b.notify");
                            b.notify();
                            System.out.println("after b.notify");
                            Thread.sleep(5000);


                        }

                        synchronized (a){
                            a.notify();
                        }
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }



            });
        }
        executorService.shutdown();
    }


    public static void printAB() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Integer a = new Integer(0);
        Integer b = new Integer(0);
        Integer c = new Integer(0);

        AtomicBoolean isWaiting = new AtomicBoolean(false);
        for (int i = 0; i < 2; i++) {
            int finalI = i;

            executorService.execute(() -> {
                Boolean first = new Boolean(true);
                while (true) {
                    if (finalI == 0) {
                        if (first) {
                            try {
                                Thread.sleep(1000);
                                first = false;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.print("A");

                        synchronized (a) {
                            a.notify();
                            try {
                                a.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }


                    } else {

                        if (first) {
                            try {
                                synchronized (a) {
                                    a.wait();
                                    first = false;
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        System.out.print("B");

                        synchronized (a) {
                            a.notify();
                            try {

                                a.wait();


                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }
            });
        }
    }
}