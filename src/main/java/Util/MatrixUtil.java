package Util;

import net.MatrixMessage;
import org.jblas.FloatMatrix;

import java.util.Map;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-02 21:07
 */
public class MatrixUtil {
    public static FloatMatrix MatrixMessage_2_FloatMatrix(MatrixMessage m){
        float[] data=new float[m.getDataCount()];
        for(int i=0;i<m.getDataCount();i++){
            data[i]=m.getData(i);
        }
        FloatMatrix tmp=new FloatMatrix();
        tmp.data=data;
        tmp.rows=m.getRow();
        tmp.columns=m.getCols();
        tmp.length=tmp.rows*tmp.columns;
        return tmp;
    }

    public static MatrixMessage FloatMatrix_2_MatrixMessage(FloatMatrix m){
        MatrixMessage.Builder matrixMessage=MatrixMessage.newBuilder();
        if(m==null){
            return matrixMessage.build();
        }
        for(float f:m.data){
            matrixMessage.addData(f);
        }
        matrixMessage.setCols(m.columns);
        matrixMessage.setRow(m.rows);
        return matrixMessage.build();
    }
}