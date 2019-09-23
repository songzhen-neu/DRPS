package data.preprocess;

import Util.DataProcessUtil;
import context.Context;
import context.WorkerContext;

import java.io.IOException;

/**
 * @program: simplePsForModelPartition
 * @description: 将源数据转化成key，value格式
 * @author: SongZhen
 * @create: 2019-09-23 13:16
 */
public class LMFMetaToLevelDB {
    public static void main(String[] args) throws IOException {
        Context.init();
        WorkerContext.init();
        DataProcessUtil.metaToDB_LMF();
    }

}