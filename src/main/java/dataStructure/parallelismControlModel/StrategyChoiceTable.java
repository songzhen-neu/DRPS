package dataStructure.parallelismControlModel;

/**
 * @program: simplePsForModelPartition
 * @description: 策略选择表，用于最优worker-selection并行控制模型
 * @author: SongZhen
 * @create: 2019-04-08 15:17
 */
public class StrategyChoiceTable {
    public float waitTime;
    public int staleness;
    public float negGain;
}