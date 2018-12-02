package DataTyleTrans;

import Gradient.GradientStructure;
import javafx.util.Pair;
import net.Gradient;

import java.util.List;
import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-02 21:07
 */
public class GradientUtil {
    public static Gradient.Builder gradientMapToProtoGradient(GradientStructure gradient){
        Gradient.Builder protoGradient=Gradient.newBuilder();
        for (Map.Entry<String,Float> entry:gradient.getGradient().entrySet()){
            net.Map.Builder map=net.Map.newBuilder();
            map.setKey(entry.getKey());
            map.setValue(entry.getValue());
            protoGradient.addGradient(map);
        }
        return protoGradient;
    }
}