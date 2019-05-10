package TestClass;

import Util.CurrentTimeUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description:测试map.get和set.contains哪个快
 * @author: SongZhen
 * @create: 2019-05-10 09:45
 */
public class TestMapAndSet {
    public static void main (String[] args){
        // 实验证明map要比set快很多

        Set<String>[] setArray=new Set[3];
        Map<String,Integer> map=new HashMap<String, Integer>();
        int echo=100000;

        //要读取的set
        Set<String> setRead=new HashSet<String>();


        Set[] setSend=new Set[3];
        Set[] setSend1=new Set[3];

        for(int i=0;i<setArray.length;i++){
            setArray[i]=new HashSet();
            setSend[i]=new HashSet();
            setSend1[i]=new HashSet();
        }

        for(int i=0;i<setArray.length;i++){
            for(int j=0;j<1000;j++){
                setArray[i].add("p"+(j+i*1000));
                map.put("p"+(j+i*1000),i);
            }
        }

        for(int i=0;i<3000;i++){
            setRead.add("p"+i);

        }






        // 按照set进行读取
        CurrentTimeUtil.setStartTime();
        for(int i=0;i<echo;i++){
            for(String str:setRead){
                for(int j=0;j<setArray.length;j++){
                    if(setArray[j].contains(str)){
                        setSend[j].add(str);
                    }
                }
            }
        }

        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("set");

        // 按照hashmap去读
        CurrentTimeUtil.setStartTime();
        for(int i=0;i<echo;i++){
            for(String str:setRead){
                if(map.keySet().contains(str)){
                    setSend1[map.get(str)].add(str);
                }
            }
        }

        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("map");


        CurrentTimeUtil.setStartTime();
        for(int i=0;i<100000;i++){
            for(String str:setRead){
                map.get(str);
            }
        }
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("map.get");



        CurrentTimeUtil.setStartTime();
        for(int i=0;i<100000;i++){
            for(String str:setRead){
                map.keySet().contains(str);
            }
        }
        CurrentTimeUtil.setEndTime();
        CurrentTimeUtil.showExecuteTime("set.contains");

        System.out.println("");


    }
}