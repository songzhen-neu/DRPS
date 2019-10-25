import Partitioning.DataPartitioning;
import dataStructure.dataPartitioning.PartitionSetting;

import java.io.IOException;

import static Partitioning.DataPartitioning.dataPartitioning;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-10 14:38
 */
public class DataOfflinePartition {
    public static void main(String[] args) throws IOException {
        PartitionSetting SVM = new PartitionSetting(3, 100000, "data/SVMData/dataset");
        PartitionSetting LiR = new PartitionSetting(5, 200000, "data/LiRData/data");
        PartitionSetting LMF = new PartitionSetting(5, 1100000, "data/LMFData/Goodbooks");
        PartitionSetting LoR = new PartitionSetting(5, 8000000, "data/LoRData/train");


        // rating每个数据集4800000
        // Goodbooks 1100000
        // train 8000000
        // dataset 140000
        // housing 4400000
        // data 200000


//        DataPartitioning.dataPartitioning(SVM);
        DataPartitioning.dataPartitioning(LoR);
//        DataPartitioning.dataPartitioning(LMF);
//        DataPartitioning.dataPartitioning(LoR);
    }
}