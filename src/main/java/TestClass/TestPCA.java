package TestClass;

import Jama.Matrix;
import com.mkobos.pca_transform.PCA;

public class TestPCA {

    public static void main(String[] args) {
        System.out.println("Running a demonstration program on some sample data ...");
        /** Training data matrix with each row corresponding to data point and
         * each column corresponding to dimension. */
        Matrix trainingData = new Matrix(new double[][]{
                {1, 2, 3, 4, 5, 6, 1, 2, 2, 3, 4, 5, 6, 2},
                {6, 5, 4, 3, 2, 1, 7, 4, 2, 3, 4, 5, 6, 4},
                {2, 2, 2, 2, 2, 2, 2, 6, 2, 3, 4, 5, 6, 3}
        });
        PCA pca = new PCA(trainingData);
        /** Test data to be transformed. The same convention of representing
         * data points as in the training data matrix is used. */
        Matrix testData = new Matrix(new double[][]{
                {1, 2, 3, 4, 5, 6, 1, 3, 2, 3, 4, 5, 6 ,1},
                {1, 2, 1, 2, 1, 2, 2, 4, 2, 3, 4, 5, 6 ,1},
                {1, 2, 1, 6, 4, 5, 5, 4, 2, 3, 4, 5, 6 ,1}});
        /** The transformed test data. */
        Matrix transformedData =
                pca.transform(testData, PCA.TransformationType.WHITENING);
        System.out.println("Transformed data (each row corresponding to transformed data point):");
        for (int r = 0; r < transformedData.getRowDimension(); r++) {
            for (int c = 0; c < transformedData.getColumnDimension(); c++) {
                System.out.print(transformedData.get(r, c));
                if (c == transformedData.getColumnDimension() - 1) continue;
                System.out.print(", ");
            }
            System.out.println("");
        }
    }
}