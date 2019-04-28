package parallelism;

import context.ServerContext;
import io.grpc.stub.StreamObserver;
import net.SFKVListMessage;

import java.io.IOException;
import java.util.Set;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-04-28 13:09
 */
public class RespTool {
    public static void respParam(StreamObserver<SFKVListMessage> resp, Set<String> neededParamIndices) {
        try {
            SFKVListMessage sfkvListMessage = ServerContext.kvStoreForLevelDB.getNeededParams(neededParamIndices);
            resp.onNext(sfkvListMessage);
            resp.onCompleted();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }
}