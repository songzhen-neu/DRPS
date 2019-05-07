package Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description: 关于集合的相关工具
 * @author: SongZhen
 * @create: 2019-01-19 14:50
 */
public class SetUtil {
    public static Set[] initSetArray(int n){
        Set[] set=new HashSet[n];
        for(int i=0;i<n;i++){
            set[i]=new HashSet();
        }
        return set;
    }

    public static List[] initListArray(int n){
        List[] lArray=new ArrayList[n];
        for(int i=0;i<n;i++){
            lArray[i]=new ArrayList();
        }
        return lArray;
    }

    public static Set List_2_Set(List list){
        Set set=new HashSet();
        for(int i=0;i<list.size();i++){
            set.add(list.get(i));
        }

        return set;
    }
}