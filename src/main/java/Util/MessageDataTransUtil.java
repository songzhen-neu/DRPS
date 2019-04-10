package Util;

import context.WorkerContext;
import net.*;
import org.jblas.FloatMatrix;

import java.util.*;

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

    public static Map<Long,Long> KeyValueListMessage_2_Map(KeyValueListMessage KeyValueListMessage ){
        Map<Long,Long> map=new HashMap<Long, Long>();
        for(int i=0;i<KeyValueListMessage.getSize();i++){
            map.put(KeyValueListMessage.getKeyValueList(i).getKey(),KeyValueListMessage.getKeyValueList(i).getValue());
        }
        return map;
    }

    public static KeyValueListMessage Map_2_KeyValueListMessage(Map<Long,Long> map){
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

    public static Map<String,Long> SLKVListMessage_2_map(SLKVListMessage slkvListMessage){
        /**
        *@Description: <string,long>类型的messageList转化为map
        *@Param: [slkvListMessage]
        *@return: java.util.Map<java.lang.String,java.lang.Long>
        *@Author: SongZhen
        *@date: 下午9:45 18-12-19
        */
        Map<String,Long> map=new HashMap<String, Long>();
        for(int i=0;i<slkvListMessage.getSize();i++){
            map.put(slkvListMessage.getList(i).getKey(),slkvListMessage.getList(i).getValue());
        }
        return map;
    }

    public static SLKVListMessage Map_2_SLKVListMessage(Map<String,Long> map){
        /**
         *@Description: <string,long>类型的messageList转化为map
         *@Param: [slkvListMessage]
         *@return: java.util.Map<java.lang.String,java.lang.Long>
         *@Author: SongZhen
         *@date: 下午9:45 18-12-19
         */
        SLKVListMessage.Builder listMessage=SLKVListMessage.newBuilder();
        listMessage.setSize(map.size());
        for(String i:map.keySet()){
            SLKVMessage.Builder message=SLKVMessage.newBuilder();
            message.setKey(i);
            message.setValue(map.get(i));
            listMessage.addList(message);
        }

        return listMessage.build();
    }


    public static SFKVListMessage Map_2_SFKVListMessage(Map<String,Float> map){
        /**
         *@Description: <string,long>类型的messageList转化为map
         *@Param: [slkvListMessage]
         *@return: java.util.Map<java.lang.String,java.lang.Long>
         *@Author: SongZhen
         *@date: 下午9:45 18-12-19
         */
        SFKVListMessage.Builder listMessage=SFKVListMessage.newBuilder();
        for(String i:map.keySet()){
            SFKVMessage.Builder message=SFKVMessage.newBuilder();
            message.setKey(i);
            message.setValue(map.get(i));
            listMessage.addList(message);
        }

        return listMessage.build();
    }

    public static Set<String> SListMessage_2_Set(SListMessage req){
        Set<String> set=new HashSet<String>();
        for(int i=0;i<req.getListCount();i++){
            set.add(req.getList(i));
        }
        return set;
    }

    public static Set<String> SSListMessage_2_Set(SSListMessage req){
        Set<String> set=new HashSet<String>();
        for(int i=0;i<req.getListCount();i++){
            set.add(req.getList(i));
        }
        return set;
    }

    public static SListMessage Set_2_SListMessage(Set<String> set){
        SListMessage.Builder sListMessage=SListMessage.newBuilder();
        for(String key:set){
            sListMessage.addList(key);
        }
        return sListMessage.build();
    }

    public static SSListMessage Set_2_SSListMessage(Set<String> set){
        SSListMessage.Builder sSListMessage=SSListMessage.newBuilder();
        for(String key:set){
            sSListMessage.addList(key);
        }
        sSListMessage.setWorkerId(WorkerContext.workerId);
        return sSListMessage.build();
    }

    public static Map<String,Float> SFKVListMessage_2_Map(SFKVListMessage sfkvListMessage){
        Map<String,Float> map=new HashMap<String, Float>();
        for(int i=0;i<sfkvListMessage.getListCount();i++){
            map.put(sfkvListMessage.getList(i).getKey(),sfkvListMessage.getList(i).getValue());
        }
        return map;
    }


    public static LIListMessage Map_2_LIListMessage(Map<Long,Integer> vAccessNum){
        LIListMessage.Builder message=LIListMessage.newBuilder();
        for(long i:vAccessNum.keySet()){
            LIMessage.Builder liMessage=LIMessage.newBuilder();
            liMessage.setL(i);
            liMessage.setI(vAccessNum.get(i));
            message.addList(liMessage.build());
        }
        message.setSize(vAccessNum.size());
        return message.build();
    }

    public static Map<Long,Integer> LIListMessage_2_Map(LIListMessage message){
        Map<Long,Integer> map=new HashMap<Long, Integer>();
        for(int i=0;i<message.getSize();i++){
            LIMessage liMessage=message.getList(i);
            map.put(liMessage.getL(),liMessage.getI());
        }
        return map;
    }

    public static Set<Long> LListMessage_2_Set(LListMessage message){
        Set<Long> set=new HashSet<Long>();
        for(int i=0;i<message.getLCount();i++){
            set.add(message.getL(i));
        }
        return set;
    }

    public static List<Set> ListSetMessage_2_ListSet(ListSetMessage lsmessage){
        List<Set> ls=new ArrayList<Set>();
        for(int i=0;i<lsmessage.getLsCount();i++){
            Set<Long> set=new LinkedHashSet<Long>();
            for(int j=0;j<lsmessage.getLs(i).getLCount();j++){
                set.add(lsmessage.getLs(i).getL(j));
            }
            ls.add(set);
        }

        return ls;
    }

    public static ListSetMessage ListSet_2_ListSetMessage(List<Set> ls){
        ListSetMessage.Builder lsMessage=ListSetMessage.newBuilder();
        for(Set<Long> set:ls){
            LListMessage.Builder llistMessage=LListMessage.newBuilder();
            for(Long l:set){
                llistMessage.addL(l);
            }
            lsMessage.addLs(llistMessage);
        }
        return lsMessage.build();
    }

    public static LListMessage LongArray_2_LListMessage(long[] longArray){
        LListMessage.Builder message=LListMessage.newBuilder();
        for(long l:longArray){
            message.addL(l);
        }

        return message.build();
    }

    public static long[] LListMessage_2_LongArray(LListMessage message){
        long[] lArray=new long[message.getLCount()];
        for(int i=0;i<lArray.length;i++){
            lArray[i]=message.getL(i);
        }
        return lArray;
    }

    public static float[] FListMessage_2_FloatArray(FListMessage message){
        float[] fArray=new float[message.getFCount()];
        for(int i=0;i<fArray.length;i++){
            fArray[i]=message.getF(i);
        }
        return fArray;
    }

    public static FListMessage FloatArray_2_FListMessage(float[] fArray){
        FListMessage.Builder message=FListMessage.newBuilder();
        for(float f:fArray){
            message.addF(f);
        }

        return message.build();
    }

    public static LSetListArrayMessage SetListArray_2_LSetListArrayMessage(List<Set>[] slArray){
        LSetListArrayMessage.Builder lSetListArrayMessage=LSetListArrayMessage.newBuilder();
        for(int i=0;i<slArray.length;i++){
            LSetListMessage.Builder slMessage=LSetListMessage.newBuilder();
            List<Set> ls=slArray[i];
            for(Set<Long> set:ls){
                LSetMessage.Builder sMessage=LSetMessage.newBuilder();

                for(Long l:set){
                    LMessage.Builder lMessage=LMessage.newBuilder();
                    lMessage.setL(l);
                    sMessage.addL(lMessage);
                }
                slMessage.addLSet(sMessage);
            }
            lSetListArrayMessage.addLSetList(slMessage);
        }
        return lSetListArrayMessage.build();
    }


    public static List<Set>[] LSetListArrayMessage_2_SetListArray(LSetListArrayMessage lSLArrayMessage){
        List<Set>[] slArray=new ArrayList[lSLArrayMessage.getLSetListCount()];
        for(int i=0;i<lSLArrayMessage.getLSetListCount();i++){
            LSetListMessage slMessage=lSLArrayMessage.getLSetList(i);
            List<Set> sl=new ArrayList<Set>();
            for(int j=0;j<slMessage.getLSetCount();j++){
                LSetMessage setMessage=slMessage.getLSet(j);
                Set<Long> set=new HashSet<Long>();
                for(int k=0;k<setMessage.getLCount();k++){
                    set.add(setMessage.getL(k).getL());
                }
                sl.add(set);
            }
            slArray[i]=sl;
        }
        return slArray;
    }
}