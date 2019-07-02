package Util;

import context.Context;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import net.SFKVListMessage;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-15 15:15
 */
public class AlgorithmUtil {
    public static void getActivateValue(SampleList batch, Map<String, Float> paramsMap, float[] value) {
        for (int l = 0; l < value.length; l++) {
            Sample sample = batch.sampleList.get(l);
            // 计算feature的value
            for (int i = 0; i < Context.featureSize; i++) {
                if (sample.feature[i] != -1) {
                    value[l] += sample.feature[i] * paramsMap.get("f" + i);
                }
            }
            for (int i = 0; i < sample.cat.length; i++) {
                if (sample.cat[i] != -1) {
                    value[l] += paramsMap.get("p" + sample.cat[i]);
                }
            }

        }
    }

    public static void getParamsMap(Map<String, Float>[] paramsMapsTemp, Map<String, Float> paramsMap, Future<SFKVListMessage>[] sfkvListMessageFuture, Logger logger) {
        for (int l = 0; l < Context.serverNum; l++) {
            logger.info(l + "barrier start");
            while (!sfkvListMessageFuture[l].isDone()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logger.info(l + "barrier end");
            try {
                paramsMapsTemp[l] = MessageDataTransUtil.SFKVListMessage_2_Map(sfkvListMessageFuture[l].get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            for (String key : paramsMapsTemp[l].keySet()) {
                // 把远程请求到的参数放到paramsMap里，这里内存是可以存得下一个batch的参数的
                paramsMap.put(key, paramsMapsTemp[l].get(key));
            }
        }
    }
}