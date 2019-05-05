package dataStructure.sample;

import java.io.Serializable;

/**
 * @program: simplePsForModelPartition
 * @description: 单个训练数据的数据结构
 * @author: SongZhen
 * @create: 2018-12-07 15:22
 */
public class Sample implements Serializable {
    public float click;
    public float feature[];
    public long cat[];
    public Sample(float[] feature,long[] cat, float click){
        this.click=click;
        this.feature=feature;
        this.cat=cat;
    }

    public Sample(long[] cat){
        this.cat=cat;
    }
}