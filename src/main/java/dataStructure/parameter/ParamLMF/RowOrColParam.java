package dataStructure.parameter.ParamLMF;

import java.io.Serializable;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-14 14:37
 */
public class RowOrColParam implements Serializable {
    public String index;
    public Float[] param;
    public RowOrColParam(int r){
        this.param=new Float[r];
    }
    public RowOrColParam(String index, Float[] param){
        this.index=index;
        this.param=param;
    }

}