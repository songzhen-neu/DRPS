package dataStructure.SparseMatrix;

import java.io.Serializable;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-05-13 20:11
 */
public class MatrixElement implements Serializable {
    public int row;
    public int col;
    public float val;

    public void set(int row, int col,float val){
        this.col=col;
        this.row=row;
        this.val=val;
    }
}