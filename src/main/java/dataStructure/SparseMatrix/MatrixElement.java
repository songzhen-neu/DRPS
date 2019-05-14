package dataStructure.SparseMatrix;

import java.io.Serializable;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-13 20:11
 */
public class MatrixElement implements Serializable {
    public long row;
    public long col;
    public float val;

    public void set(long row, long col,float val){
        this.col=col;
        this.row=row;
        this.val=val;
    }
}