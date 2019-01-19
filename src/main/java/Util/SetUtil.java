package Util;

import java.util.HashSet;
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
}