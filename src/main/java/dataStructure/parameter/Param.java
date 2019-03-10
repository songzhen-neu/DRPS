package dataStructure.parameter;

import java.io.Serializable;

/**
 * @program: simplePsForModelPartition
 * @description: 参数存储采用key, value的形式
 * @author: SongZhen
 * @create: 2019-03-10 14:53
 */
public class Param implements Serializable {
    public String key;
    public float value;
    public Param(String key,float value){
        this.key=key;
        this.value=value;
    }
}