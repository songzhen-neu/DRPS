package Util;

import java.io.File;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2018-12-07 16:28
 */
public class FileUtil {
    public static void deleteFile(File deleteFile){
        if(deleteFile.isDirectory()){
            File[] files=deleteFile.listFiles();
            for(int i=0;i<files.length;i++){
                File file=files[i];
                deleteFile(file);
            }
        }
        else if(deleteFile.exists()){
            deleteFile.delete();
        }
    }
}