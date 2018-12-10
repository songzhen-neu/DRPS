package dataStructure.sample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: simplePsForModelPartition
 * @description: 训练集数据结构
 * @author: SongZhen
 * @create: 2018-12-07 15:24
 */
public class SampleList implements Serializable {
    public List<Sample> sampleList=new ArrayList<Sample>();
    public int sparseDimSize;
    public int featureSize;
    public int catSize;
    public int sampleListSize;
    public int samplePrunedSize;
    public SampleList(){

    }
    public SampleList(int featureSize,int catSize){
        this.featureSize=featureSize;
        this.catSize=catSize;
    }

    public void showSparseDimSize(){
        System.out.println("sparse dimensions size:"+this.sparseDimSize);
    }
}