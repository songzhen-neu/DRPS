package store;

import com.google.common.collect.Maps;
import org.jblas.FloatMatrix;

import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-03 10:41
 */
public class KVStore {
    private FloatMatrix sum= new FloatMatrix();


    public synchronized  void sumAFMatrix(FloatMatrix val){
        sum.add(val);
    }

}