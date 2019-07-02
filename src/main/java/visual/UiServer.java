package visual;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.iki.elonen.NanoHTTPD;
import io.grpc.Context;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;

import javax.sql.rowset.spi.SyncResolver;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-06-24 14:47
 */
public class UiServer extends NanoHTTPD implements UiServerGrpc.UiServer {
    static Logger logger = LoggerFactory.getLogger(UiServer.class);

    ObjectMapper objectMapper = new ObjectMapper();

    ConcurrentHashMap<String, List<Float>> xs = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, List<Float>> ys = new ConcurrentHashMap<>();

    ConcurrentMap<String, List<Double>> list = new ConcurrentHashMap<String, List<Double>>();

    ConcurrentMap<String, Integer> workerProcess = new ConcurrentHashMap<String, Integer>();


    Server server;

    public UiServer() throws IOException {
        super(8888);
        init();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        server = ServerBuilder.forPort(8990).addService(UiServerGrpc.bindService(this)).build();
        try {
            server.start();
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public void init() {
        for (int i = 0; i < 3; i++) {
            workerProcess.put("w"+i, 0);
        }
    }

    public void close() {
        server.shutdown();
    }

    public static void main(String[] args) {
        try {
            new UiServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            Map<String, String> params = session.getParms();
            Map<String, String> body = new HashMap<String, String>();
            session.parseBody(body);
            if ("data".equals(params.get("act"))) {
                // /act=data&key=fc0,fc1
                String keys = params.get("key");
                Map<String, Float> step = Maps.newHashMap();
                try {
                    step = objectMapper.readValue(body.get("postData"), Map.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Collection<String> set = xs.keySet();
                if (StringUtils.isNotBlank(keys)) {
                    set = Lists.newArrayList(keys.split(","));
                }
                Map<String, Object> result = Maps.newHashMap();
                for (String key : set) {
                    List<Float> x = xs.get(key);
                    List<Float> y = ys.get(key);
                    if (x == null || y == null) {
                        continue;
                    }
                    Map<String, List<Float>> tmp = Maps.newHashMap();
                    tmp.put("x", Lists.newArrayList());
                    tmp.put("y", Lists.newArrayList());
                    for (int i = 0; i < y.size(); i++) {
                        if (!step.containsKey(key) || y.get(i) > NumberUtils.toFloat(String.valueOf(step.get(key)), 0f)) {
                            tmp.get("x").add(x.get(i));
                            tmp.get("y").add(y.get(i));
                        }
                    }
                    result.put(key, tmp);
                }
                return newFixedLengthResponse(objectMapper.writeValueAsString(result));
            } else if ("data1".equals(params.get("act"))) {
//                Map<String,List<Double>> result=new HashMap<String, List<Double>>();
//                for(String index:list.keySet()){
//                    result.put(index,list.get(index));
//                }
                return newFixedLengthResponse(objectMapper.writeValueAsString(list));
            } else if ("data2".equals(params.get("act"))) {

                return newFixedLengthResponse(objectMapper.writeValueAsString(workerProcess));
            } else {
                return newFixedLengthResponse(TestDataSet.readToString(UiServer.class.getResource("").getPath() + "../../../src/main/resources/web/index1.html"));
            }
        } catch (Exception e) {
            logger.error("http error", e);
            return newFixedLengthResponse(e.getMessage());
        }
//        return newFixedLengthResponse(TestDataSet.readToString(UiServer.class.getResource("").getPath() + "../../../src/main/resources/web/index1.html"));
    }

    @Override
    public void plot(PlotMessage req, StreamObserver<PlotMessage> resp) {
        String key = req.getId();
        // 如果存在，返回map中的值，不替换
        List<Float> x = xs.putIfAbsent(key, Lists.newArrayList());
        List<Float> y = ys.putIfAbsent(key, Lists.newArrayList());
        if (x == null) {
            x = xs.putIfAbsent(key, Lists.newArrayList());
        }
        if (y == null) {
            y = ys.putIfAbsent(key, Lists.newArrayList());
        }
        // 断言，如果true，则程序继续执行，否则抛出AssertionError的错误
        assert y != null;
        synchronized (y) {
            // addAll将集合中的元素加入，迭代器
            y.addAll(req.getData().getYList());
        }
        assert x != null;
        synchronized (x) {
            x.addAll(req.getData().getXList());
        }
        resp.onNext(PlotMessage.newBuilder().build());
        resp.onCompleted();

    }

    @Override
    public void plotScatterGraph(plotScatterGraphMessage req, StreamObserver<Flag> resp) {
        List<Double> l1x = new ArrayList<Double>();
        List<Double> l1y = new ArrayList<Double>();
        List<Double> l2x = new ArrayList<Double>();
        List<Double> l2y = new ArrayList<Double>();
        for (int j = 0; j < req.getPointCollection(0).getXCount(); j++) {
            l1x.add(req.getPointCollection(0).getX(j));
            l1y.add(req.getPointCollection(0).getY(j));
        }

        for (int j = 0; j < req.getPointCollection(1).getXCount(); j++) {
            l2x.add(req.getPointCollection(1).getX(j));
            l2y.add(req.getPointCollection(1).getY(j));
        }

        list.put("l1x", l1x);
        list.put("l1y", l1y);
        list.put("l2x", l2x);
        list.put("l2y", l2y);
        resp.onNext(Flag.newBuilder().build());
        resp.onCompleted();
    }

    @Override
    public void plotWorkerProcess(workerProcessMessage req, StreamObserver<Flag> resp) {
        workerProcess.put("w"+req.getWorkerid(), req.getCuriteration());
        resp.onNext(Flag.newBuilder().build());
        resp.onCompleted();
    }


}