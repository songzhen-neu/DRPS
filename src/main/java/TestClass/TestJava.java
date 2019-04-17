package TestClass;

import dataStructure.parameter.Param;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-04-09 15:51
 */
public class TestJava {

    public static void main(String[] args){

        Param param=new Param("a",2);
        Param param1=new Param("a",2);
        String a=new String("haha");
        // 对于基本类型的equals就是判断两个值是不是相等
        System.out.println(param.equals(param1));

        // 对于java提供的所有基础类型，equals函数都是比较值
        Pair<String,String> pair=new Pair<String, String>("a1","a");
        Pair<String,String> pair2=new Pair<String, String>("a1","a");
        System.out.println(pair.equals(pair2));


        // Set.contains是使用equals函数进行判断的，也就是对于基本类型，contains都是比较的值，对于自定义类才比较的对象
        Set<String> set=new HashSet<String>();
        String str="haha2";
        String str2=new String("haha3");
        set.add("haha1");
        set.add(str);
        set.add(str2);
        System.out.println(set.contains("haha1"));
        System.out.println(set.contains("haha2"));
        System.out.println(set.contains("haha3"));

        Integer inta=1231;
        Integer intb=1231;
        System.out.println(inta==intb);






    }
}