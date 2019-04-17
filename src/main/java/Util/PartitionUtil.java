package Util;

import context.Context;
import context.WorkerContext;
import dataStructure.sample.Sample;
import dataStructure.sample.SampleList;
import net.PSWorker;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @program: simplePsForModelPartition
 * @description: 进行划分的工具包
 * @author: SongZhen
 * @create: 2019-01-18 10:32
 */
public class PartitionUtil {
    static Logger logger = LoggerFactory.getLogger(PartitionUtil.class.getName());
    static DB db = WorkerContext.kvStoreForLevelDB.getDb();
    static Set<Integer> batchSampledRecord = new HashSet<Integer>();
    static Set<Long> catPrunedRecord = new HashSet<Long>();
    // 这个是放在内存里的vAccessNum
    static Map<Long, Integer> vAccessNum = new HashMap<Long, Integer>();


    public static Set[] partitionV() throws IOException, ClassNotFoundException {
        boolean isInited = false;
        float Ti_com = 0;
        float Ti_disk = 0;
        int insertI = 0;
        // 本地存储哪台机器（server）存了哪些参数
        Set[] vSet = SetUtil.initSetArray(Context.serverNum);

        // 统计每个参数被本地的batch访问的次数，并放到worker的数据库里，以vAccessNum开头,
        logger.info("build local VAccessNum start");
        buildVAccessNum();
        DataProcessUtil.showVAccessNum(vAccessNum);
        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();
        logger.info("getMaxMinValue of features end");


        // 上述是已经采样完的数据的关于V的访问的统计，其中batchRecord是采样的batch的index
        // 下面开始进行维度的剪枝，返回的是server计算完成之后，被剪枝后的维度
        // 所以每台机器都要向master发送采样后，每个V被访问的次数
        logger.info("send local VAN and get CarPrunedRecord");
        // 是获取剪枝后的维度，每个worker都向server发送每个维度的访问次数
        catPrunedRecord = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushVANumAndGetCatPrunedRecord(vAccessNum);
        logger.info("prunedVSet:" + catPrunedRecord.size());


        // 下面取出j=1，放在第insertI台机器上
        long j_last = 0;
        List<Set> partitionedVSet;
        // 剪枝后的维度就是要划分的维度
        for (long j : catPrunedRecord) {
            if (!isInited) {
                // 也就是初始化Ticom和Ti_disk
                Ti_com = getInitTiComInMemory(catPrunedRecord);


                Ti_disk = 0;


                isInited = true;
            } else {
                // 统计本机访问Vi的次数
                float T_localAccessVj = getVjAccessNumInMemory(j_last);
                // 所有的worker都要pull一下划分后的vset
                logger.info("pullPartitionedVSet start");
                partitionedVSet = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pullPartitionedVset(insertI);
                logger.info("pullPartitionedVSet end");
                WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).barrier();
                // 下面是对disk时间的计算
                if (partitionedVSet.size() == 0) {
                    if (insertI == WorkerContext.workerId) {
                        logger.info("addInitedPartitionedVSet start");
                        WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).addInitedPartitionedVSet(j_last, insertI);
                        logger.info("addInitedPartitionedVSet end");
                    }
                } else {

                    // 遍历数据集并开始统计，并返回对磁盘的访问次数
                    float[] diskAccessForV = getDiskAccessTimeForV(partitionedVSet, j_last);

                    // 每个worker都将diskAccessForV传递给server，server选择将j加入到vi的某个划分中（或者自己成为一个新的划分）
                    logger.info("pushDiskAccessForV start");
                    Ti_disk = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushDiskAccessForV(diskAccessForV, insertI, j_last);
                    logger.info("pushDiskAccessForV end");


                }
                // 统计其他机器访问Vi的次数,对网络通信时间的计算
                if (insertI == WorkerContext.workerId) {
                    // 这些都还是对j_last插入后，做的Tdisk和Tcom的更新计算
                    logger.info("pullOtherWorkerAccessForVi start");
                    float accessNum_otherWorkers = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pullOtherWorkerAccessForVi();
                    logger.info("pullOtherWorkerAccessForVi end");
//                    Ti_com = Ti_com - T_localAccessVj + accessNum_otherWorkers;   // 注释了，只用网络通信时间

                    // 下面开始计算disk的时间,也是只修改插入的Tdisk的值。
                    // 先从server中获取vSet[insertId]的参数分配

                } else {
                    logger.info("pushLocalViAccessNum start");
                    WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushLocalViAccessNum(T_localAccessVj);
                    logger.info("pushLocalViAccessNum end");
                }
            }


            PSWorker psWorker = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId);
            logger.info("sentInitedT start");
            insertI = psWorker.sentInitedT(Ti_com * Context.netTrafficTime + Ti_disk);
            logger.info("sentInitedT end");

            logger.info("insert " + j + " into " + insertI);
            // 发送给server master，然后选出一个耗时最短的机器i，然后作为加入j的机器

            vSet[insertI].add(j);
            j_last = j;

//            System.out.println("setSize:"+(vSet[0].size()+vSet[1].size()+vSet[2].size()));
        }

        // j_last没有插入
        if (vSet[insertI].size() != 0) {
            // 统计本机访问Vi的次数
            float T_localAccessVj = getVjAccessNumInMemory(j_last);
            // 所有的worker都要pull一下划分后的vset
            logger.info("pullPartitionedVSet start");
            partitionedVSet = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pullPartitionedVset(insertI);
            logger.info("pullPartitionedVSet end");
            // 遍历数据集并开始统计，并返回对磁盘的访问次数
            float[] diskAccessForV = getDiskAccessTimeForV(partitionedVSet, j_last);

            // 每个worker都将diskAccessForV传递给server，server选择将j加入到vi的某个划分中（或者自己成为一个新的划分）
            logger.info("pushDiskAccessForV start");
            Ti_disk = WorkerContext.psRouterClient.getPsWorkers().get(Context.masterId).pushDiskAccessForV(diskAccessForV, insertI, j_last);
            logger.info("pushDiskAccessForV end");
        }
        for (Object l : vSet[WorkerContext.workerId]) {
            System.out.println("lvSet:" + l);
        }
        return vSet;

    }


    private static float[] getDiskAccessTimeForV(List<Set> ls_partitionedVSet, long j_last) {
        /**
         *@Description: 还没写完，有点乱，list set其实就已经是要插入的V了。
         *@Param: [ls_partitionedVSet, insertId, j_last]
         *@return: long[]
         *@Author: SongZhen
         *@date: 上午11:26 19-1-26
         */
        // 因为是静态的上下文，所以diskAccessForV数组的元素全为0

        float[] diskAccessForV = new float[ls_partitionedVSet.size() + 1];

        // 需要定义长度为n的list set数组
        List<Set>[] lsArray = new ArrayList[ls_partitionedVSet.size() + 1];

        // 同来存储各种情况的各个partition的访问次数
        List[] accessTimeOfEachPartition = SetUtil.initListArray(ls_partitionedVSet.size() + 1);
        for (int i = 0; i < (ls_partitionedVSet.size() + 1); i++) {
            for (int j = 0; j < ls_partitionedVSet.size(); j++) {
                accessTimeOfEachPartition[i].add(0);
            }
            if (i == ls_partitionedVSet.size()) {
                accessTimeOfEachPartition[i].add(0);
            }
        }


        // 构建所有组合情况的list set，原Vi有n个partitions，现在组合就有n+1个（因为j可能单独成一个set）
        for (int m = 0; m < (ls_partitionedVSet.size() + 1); m++) {
            List<Set> ls_partitionedVSet_temp = TypeExchangeUtil.copyListSet(ls_partitionedVSet);
            // 如果是0～(m-1)，则说明是加入到原来的partition里面，m的时候是创建新的partition来存放j_last
            if (m < ls_partitionedVSet.size()) {
                ls_partitionedVSet_temp.get(m).add(j_last);
            } else {
                Set<Long> set = new HashSet<Long>();
                set.add(j_last);
                ls_partitionedVSet_temp.add(set);
            }
            lsArray[m] = ls_partitionedVSet_temp;
        }


        for (int i : batchSampledRecord) {
            try {
                SampleList sampleList = (SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch" + i).getBytes()));
                Set<Long> batchCatSet = new HashSet<Long>();

                // 把batch访问的cat放到batchCatSet里
                for (Sample sample : sampleList.sampleList) {
                    long[] cat = sample.cat;
                    for (long l : cat) {
                        batchCatSet.add(l);
                    }

                }

                // 下面要填充accessTimeOfEachPartition这个数据结构，也就是各个partition的访问时间
                for (int i_accessTime = 0; i_accessTime < lsArray.length; i_accessTime++) {
                    List<Set> set_accessTime = lsArray[i_accessTime];
                    for (Set<Long> set_temp : set_accessTime) {
                        for (long l : batchCatSet) {
                            if (set_temp.contains(l)) {
                                int index_accessTime = set_accessTime.indexOf(set_temp);
                                int num = (Integer) accessTimeOfEachPartition[i_accessTime].get(index_accessTime);
                                accessTimeOfEachPartition[i_accessTime].set(index_accessTime, ++num);
                                break;
                            }
                        }
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

        // 等所有batch统计结束后，计算各个新的Vi的访问时间
        for (int i = 0; i < accessTimeOfEachPartition.length; i++) {
            float sum = 0;
            for (int j = 0; j < accessTimeOfEachPartition[i].size(); j++) {
                int accessNum = (Integer) accessTimeOfEachPartition[i].get(j);

                float accessTime = 0;
                if (lsArray[i].get(j).size() > 1) {
                    accessTime = (lsArray[i].get(j).size() * Context.singleParamOfSetSize_bytes + Context.setParamBaseSize_bytes) * Context.diskAccessTime + Context.diskSeekTime;
                } else {
                    accessTime = Context.floatSize_bytes * Context.diskAccessTime + Context.diskSeekTime;
                }
                sum += accessNum * accessTime;
            }
            diskAccessForV[i] += sum;

        }

        return diskAccessForV;
    }

    private static float getVjAccessNum(long j) throws IOException, ClassNotFoundException {
        int num = 0;
        if (db.get(("vAccessNum" + j).getBytes()) != null) {
            num = (Integer) TypeExchangeUtil.toObject(db.get(("vAccessNum" + j).getBytes()));
        } else {
            num = 0;
        }
        return num;
    }

    private static float getVjAccessNumInMemory(long j) throws IOException, ClassNotFoundException {
        int num = 0;
        if (vAccessNum.get(j) != null) {
            num = vAccessNum.get(j);
        } else {
            num = 0;
        }
        return num;
    }


    private static float getInitTiComInMemory(Set<Long> catPrunedRecord) {
        /**
         *@Description: 初始的Ticom，就是所有的本地的需要访问的参数的次数。
         * 也就是看每个参数被batch访问次数之和
         *@Param: [catPrunedRecord]
         *@return: float
         *@Author: SongZhen
         *@date: 上午8:22 19-1-22
         */
        float sum = 0;
        for (long l : catPrunedRecord) {
            if (vAccessNum.get(l) != null) {
                sum += vAccessNum.get(l);
            }
        }
        return sum;
    }

    private static float getInitTiCom() throws IOException, ClassNotFoundException {
        float sum = 0;
        for (int i = 0; i < Context.sparseDimSize; i++) {
            if (db.get(("vAccessNum" + i).getBytes()) != null) {
                sum += (Integer) TypeExchangeUtil.toObject(db.get(("vAccessNum" + i).getBytes()));
            }
        }

        return sum;
    }

    private static void buildVAccessNum() {
        DB db = WorkerContext.kvStoreForLevelDB.getDb();
        Set<Long> set = new HashSet<Long>();
        int num_ContainsBatchPruned = WorkerContext.sampleBatchListSize / WorkerContext.sampleBatchListPrunedSize;


        // 遍历数据并统计Ui访问的参数v的个数
        for (int i = 0; i < WorkerContext.sampleBatchListPrunedSize; i++) {
            try {
                int m = RandomUtil.getIntRandomFromZeroToN(num_ContainsBatchPruned);
                int index = i * (num_ContainsBatchPruned) + m;
                SampleList sampleBatch = (SampleList) TypeExchangeUtil.toObject(db.get(("sampleBatch" + index).getBytes()));
                batchSampledRecord.add(i * (num_ContainsBatchPruned) + m);

                // 这里的set是采样后的每个sampleBatch
                buildVSetOfBatch(set, sampleBatch);
                // 遍历set，然后更新db里的对V的访问次数，db里没有出现的维度，说明本地数据集对这个维度没有访问
                // 这个是基于磁盘的
//                buildVSetAccessNumOfBatch(set);
                // 由于经过了采样和剪枝，那么其实可以基于内存做
                buildVSetAccessNumOfBatchInMemory(set);
                set.clear();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private static void buildVSetOfBatch(Set<Long> set, SampleList sampleBatch) {
        /**
         *@Description: 这个batch访问的所有稀疏维度cat的集合
         *@Param: [set, sampleBatch]
         *@return: void
         *@Author: SongZhen
         *@date: 上午9:29 19-1-21
         */
        for (Sample sample : sampleBatch.sampleList) {
            long[] catList = sample.cat;
            for (long cat : catList) {
                if (cat != -1) {
                    set.add(cat);
                }
            }
        }

    }

    private static void buildVSetAccessNumOfBatch(Set<Long> set) {
        try {
            for (long i : set) {
                if (db.get(("vAccessNum" + i).getBytes()) != null) {
                    Integer viAccessNum = (Integer) TypeExchangeUtil.toObject(db.get(("vAccessNum" + i).getBytes()));
                    viAccessNum++;
                    db.put(("vAccessNum" + i).getBytes(), TypeExchangeUtil.toByteArray(viAccessNum));
//                    System.out.println("i:"+i+"viAccessNum:"+viAccessNum);
                } else {
                    db.put(("vAccessNum" + i).getBytes(), TypeExchangeUtil.toByteArray(1));
                }


            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static void buildVSetAccessNumOfBatchInMemory(Set<Long> set) {
        for (long i : set) {
            if (vAccessNum.get(i) != null) {
                int num = vAccessNum.get(i);
                num++;
                vAccessNum.put(i, num);
            } else {
                vAccessNum.put(i, 1);
            }


        }
    }


}