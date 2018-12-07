package Util;

import net.KeyValueListMessage;
import net.KeyValueMessage;
import net.MatrixMessage;
import org.jblas.FloatMatrix;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-02 21:07
 */
public class MessageDataTransUtil {
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

    public static Map<Long,Integer> KeyValueListMessage_2_Map(KeyValueListMessage KeyValueListMessage ){
        Map<Long,Integer> map=new HashMap<Long, Integer>();
        for(int i=0;i<KeyValueListMessage.getSize();i++){
            map.put(KeyValueListMessage.getKeyValueList(i).getKey(),KeyValueListMessage.getKeyValueList(i).getValue());
        }
        return map;
    }

    public static KeyValueListMessage Map_2_KeyValueListMessage(Map<Long,Integer> map){
        KeyValueListMessage.Builder keyValueListMessageBuild=KeyValueListMessage.newBuilder();
        for(long i:map.keySet()){
            KeyValueMessage.Builder keyValueMessageBuilder=KeyValueMessage.newBuilder();
            keyValueMessageBuilder.setKey(i);
            keyValueMessageBuilder.setValue(map.get(i));
            keyValueListMessageBuild.addKeyValueList(keyValueMessageBuilder);
        }
        keyValueListMessageBuild.setSize(map.size());
        return keyValueListMessageBuild.build();
    }

}