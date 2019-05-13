import Util.DataProcessUtil;
import context.Context;
import context.WorkerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import paramPartition.ParamPartition;

import java.io.IOException;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-13 15:23
 */
public class WorkerForLMF {
    public static void main(String[] args) throws IOException,ClassNotFoundException {
        Logger logger=LoggerFactory.getLogger(WorkerForLinearRegression.class);
        Context.init();
        WorkerContext.init();

        // 先处理数据并放到key-value数据库中
        // 处理完的key是batchLMF0,value是matrix类型的
        DataProcessUtil.metaToDB_LMF();

        // 需要先对参数维度进行划分
        ParamPartition.partitionV_LMF();


    }
}