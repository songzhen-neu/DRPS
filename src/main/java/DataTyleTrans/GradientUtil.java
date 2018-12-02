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
public class ListUtil {
    public static void gradientMapToProtoGradient(GradientStructure gradient){
        Gradient.Builder protoGradient=Gradient.newBuilder();
        for (<String,Float> pair:gradient.getGradient()){
            protoGradient.addData(map.)
        }
    }
}