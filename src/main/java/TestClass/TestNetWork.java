package TestClass;

import context.WorkerContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-20 15:17
 */
public class TestNetWork {
    public static void test(){
        /**
        *@Description: 用来测试网络通不通
        *@Param: []
        *@return: void
        *@Author: SongZhen
        *@date: 下午3:19 18-12-20
        */
        Set<String> set=new HashSet<String>();
        set.add("catParam"+1);
        Map<String,Float> map1=WorkerContext.psRouterClient.getPsWorkers().get(2).getNeededParams(set);
    }

    public static void testNetWorkTime(){

    }
}