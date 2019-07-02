package TestClass;

import visual.UiClient;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-06-24 16:04
 */
public class TestPlot {
    public static void main(String[] args){
        for(int i=1;i<1000;i++){
            try {
                Thread.sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            UiClient.ins().plot("loss",i,i);
        }

    }
}