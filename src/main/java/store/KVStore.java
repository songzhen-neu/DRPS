package store;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.Synchronized;
import org.jblas.FloatMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-03 10:41
 */
@Data
public class KVStore {
    private Map<String, FloatMatrix> sum = new ConcurrentHashMap<String, FloatMatrix>();
    private AtomicLong l = new AtomicLong(0);
    Logger logger = LoggerFactory.getLogger(KVStore.class);
    private Map<Long, Integer> mergeDim = new ConcurrentHashMap<Long, Integer>();


    @Synchronized
    public void sumAFMatrix(FloatMatrix val) {
        l.incrementAndGet();
        if (sum.size() == 0) {
            sum.put("1", val);
            logger.info("sum.size=0");

        } else {
            sum.get("1").addi(val);
            logger.info("sum.size!=0");
        }

    }

    @Synchronized
    public void mergeDim(Map<Long,Integer> map){
        for(long i:map.keySet()){
            if(mergeDim.keySet().contains(i)){
                mergeDim.put(i,mergeDim.get(i)+map.get(i));
            }else {
                mergeDim.put(i,map.get(i));
            }
        }
    }


}