package TestClass;


import dataStructure.parameter.Param;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-03-28 14:49
 */
public class TestSomeHypeParameter {
    public static void main(String[] args) throws IOException {
        testSize();
        testSeekTime();


    }

    private static void testSeekTime() throws IOException{
        TestSeekTime.write();
        TestSeekTime.seek();
    }

    private static void testSize() throws IOException{
        Param param_catParam=new Param("catParam1",0.01f);
        Param param_feat=new Param("featParam1",0.01f);
        Long l_Long=new Long(1);
        long l_long=1;
        Set<Param> set_param_1=new HashSet<Param>();
        set_param_1.add(param_catParam);
        Set<Param> set_param_5=new HashSet<Param>();
        Set<Param> set_param_10=new HashSet<Param>();

        for(int i=0;i<5;i++){
            set_param_5.add(new Param("catParam"+i,0.01f));
        }

        for(int i=0;i<10;i++){
            set_param_10.add(new Param("catParam"+i,0.01f));
        }
        Float f_Float=new Float(1);
        float f_float=1;

        TestSizeUtil.showSizeOfObject("param_catParam",param_catParam);
        TestSizeUtil.showSizeOfObject("param_feat",param_feat);
        TestSizeUtil.showSizeOfObject("l_Long",l_Long);
        TestSizeUtil.showSizeOfObject("l_long",l_long);
        TestSizeUtil.showSizeOfObject("set_param_1",set_param_1);
        TestSizeUtil.showSizeOfObject("set_param_5",set_param_5);
        TestSizeUtil.showSizeOfObject("set_param_10",set_param_10);
        TestSizeUtil.showSizeOfObject("f_Float",f_Float);
        TestSizeUtil.showSizeOfObject("f_float",f_float);

        // 测试结果
        // [INFO] 2019-03-28 15:11:08,314 [TestClass.TestSizeUtil]-[showSizeOfObject line:31] Bytes size of param_catParam:101
        // [INFO] 2019-03-28 15:11:08,316 [TestClass.TestSizeUtil]-[showSizeOfObject line:31] Bytes size of param_feat:102
        // [INFO] 2019-03-28 15:11:08,317 [TestClass.TestSizeUtil]-[showSizeOfObject line:31] Bytes size of l_Long:82
        // [INFO] 2019-03-28 15:11:08,317 [TestClass.TestSizeUtil]-[showSizeOfObject line:31] Bytes size of l_long:82
        // [INFO] 2019-03-28 15:11:08,318 [TestClass.TestSizeUtil]-[showSizeOfObject line:31] Bytes size of set_param_1:150
        // [INFO] 2019-03-28 15:11:08,318 [TestClass.TestSizeUtil]-[showSizeOfObject line:31] Bytes size of set_param_5:238
        // [INFO] 2019-03-28 15:11:08,318 [TestClass.TestSizeUtil]-[showSizeOfObject line:31] Bytes size of set_param_10:348

        // 即set转化成byte的时候，基础占用128字节，每个Param占用22字节

    }
}