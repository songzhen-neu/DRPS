package TestClass;

import Util.TypeExchangeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @program: simplePsForModelPartition
 * @description: TestSize
 * @author: SongZhen
 * @create: 2019-03-28 14:42
 */
public class TestSizeUtil {
    static Logger logger=LoggerFactory.getLogger(TestSizeUtil.class.getName());
    public static int SizeOfObject(Object o)throws IOException {
        /**
        *@Description: 测试object的大小，通过转化成byte流来计算
        *@Param: []
        *@return: void
        *@Author: SongZhen
        *@date: 下午2:43 19-3-28
        */
        byte[] bytes=TypeExchangeUtil.toByteArray(o);
        return bytes.length;
    }

    public static void showSizeOfObject (String describe,Object o) throws IOException{
        int size=SizeOfObject(o);
        logger.info("Bytes size of "+describe+":"+size);
    }
}