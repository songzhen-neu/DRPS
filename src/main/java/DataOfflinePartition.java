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
        PartitionSetting LiR = new PartitionSetting(3, 100000, "data/LiRData/housing");
        PartitionSetting LMF = new PartitionSetting(3, 100000, "data/LMFData/Goodbooks");
        PartitionSetting LoR = new PartitionSetting(3, 100000, "data/LoRData/train");


        DataPartitioning.dataPartitioning(SVM);
        DataPartitioning.dataPartitioning(LiR);
        DataPartitioning.dataPartitioning(LMF);
        DataPartitioning.dataPartitioning(LoR);
    }
}