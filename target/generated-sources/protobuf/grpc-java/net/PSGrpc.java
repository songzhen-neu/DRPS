package net;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 0.14.0)",
    comments = "Source: ps.proto")
public class PSGrpc {

  private PSGrpc() {}

  public static final String SERVICE_NAME = "net.PS";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<net.MatrixMessage,
      net.MatrixMessage> METHOD_PUSH_AFMATRIX =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "net.PS", "PushAFMatrix"),
          io.grpc.protobuf.ProtoUtils.marshaller(net.MatrixMessage.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(net.MatrixMessage.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<net.KeyValueListMessage,
      net.PartitionListMessage> METHOD_AFMATRIX_DIM_PARTITION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "net.PS", "AFMatrixDimPartition"),
          io.grpc.protobuf.ProtoUtils.marshaller(net.KeyValueListMessage.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(net.PartitionListMessage.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<net.SListMessage,
      net.SLKVListMessage> METHOD_GET_INDEX_OF_SPARSE_DIM =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "net.PS", "GetIndexOfSparseDim"),
          io.grpc.protobuf.ProtoUtils.marshaller(net.SListMessage.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(net.SLKVListMessage.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PSStub newStub(io.grpc.Channel channel) {
    return new PSStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PSBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PSBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static PSFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PSFutureStub(channel);
  }

  /**
   */
  public static interface PS {

    /**
     */
    public void pushAFMatrix(net.MatrixMessage request,
        io.grpc.stub.StreamObserver<net.MatrixMessage> responseObserver);

    /**
     */
    public void aFMatrixDimPartition(net.KeyValueListMessage request,
        io.grpc.stub.StreamObserver<net.PartitionListMessage> responseObserver);

    /**
     */
    public void getIndexOfSparseDim(net.SListMessage request,
        io.grpc.stub.StreamObserver<net.SLKVListMessage> responseObserver);
  }

  @io.grpc.ExperimentalApi
  public static abstract class AbstractPS implements PS, io.grpc.BindableService {

    @java.lang.Override
    public void pushAFMatrix(net.MatrixMessage request,
        io.grpc.stub.StreamObserver<net.MatrixMessage> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_PUSH_AFMATRIX, responseObserver);
    }

    @java.lang.Override
    public void aFMatrixDimPartition(net.KeyValueListMessage request,
        io.grpc.stub.StreamObserver<net.PartitionListMessage> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_AFMATRIX_DIM_PARTITION, responseObserver);
    }

    @java.lang.Override
    public void getIndexOfSparseDim(net.SListMessage request,
        io.grpc.stub.StreamObserver<net.SLKVListMessage> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_GET_INDEX_OF_SPARSE_DIM, responseObserver);
    }

    @java.lang.Override public io.grpc.ServerServiceDefinition bindService() {
      return PSGrpc.bindService(this);
    }
  }

  /**
   */
  public static interface PSBlockingClient {

    /**
     */
    public net.MatrixMessage pushAFMatrix(net.MatrixMessage request);

    /**
     */
    public net.PartitionListMessage aFMatrixDimPartition(net.KeyValueListMessage request);

    /**
     */
    public net.SLKVListMessage getIndexOfSparseDim(net.SListMessage request);
  }

  /**
   */
  public static interface PSFutureClient {

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<net.MatrixMessage> pushAFMatrix(
        net.MatrixMessage request);

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<net.PartitionListMessage> aFMatrixDimPartition(
        net.KeyValueListMessage request);

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<net.SLKVListMessage> getIndexOfSparseDim(
        net.SListMessage request);
  }

  public static class PSStub extends io.grpc.stub.AbstractStub<PSStub>
      implements PS {
    private PSStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PSStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PSStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PSStub(channel, callOptions);
    }

    @java.lang.Override
    public void pushAFMatrix(net.MatrixMessage request,
        io.grpc.stub.StreamObserver<net.MatrixMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_PUSH_AFMATRIX, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void aFMatrixDimPartition(net.KeyValueListMessage request,
        io.grpc.stub.StreamObserver<net.PartitionListMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_AFMATRIX_DIM_PARTITION, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void getIndexOfSparseDim(net.SListMessage request,
        io.grpc.stub.StreamObserver<net.SLKVListMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_GET_INDEX_OF_SPARSE_DIM, getCallOptions()), request, responseObserver);
    }
  }

  public static class PSBlockingStub extends io.grpc.stub.AbstractStub<PSBlockingStub>
      implements PSBlockingClient {
    private PSBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PSBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PSBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PSBlockingStub(channel, callOptions);
    }

    @java.lang.Override
    public net.MatrixMessage pushAFMatrix(net.MatrixMessage request) {
      return blockingUnaryCall(
          getChannel(), METHOD_PUSH_AFMATRIX, getCallOptions(), request);
    }

    @java.lang.Override
    public net.PartitionListMessage aFMatrixDimPartition(net.KeyValueListMessage request) {
      return blockingUnaryCall(
          getChannel(), METHOD_AFMATRIX_DIM_PARTITION, getCallOptions(), request);
    }

    @java.lang.Override
    public net.SLKVListMessage getIndexOfSparseDim(net.SListMessage request) {
      return blockingUnaryCall(
          getChannel(), METHOD_GET_INDEX_OF_SPARSE_DIM, getCallOptions(), request);
    }
  }

  public static class PSFutureStub extends io.grpc.stub.AbstractStub<PSFutureStub>
      implements PSFutureClient {
    private PSFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PSFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PSFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PSFutureStub(channel, callOptions);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<net.MatrixMessage> pushAFMatrix(
        net.MatrixMessage request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_PUSH_AFMATRIX, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<net.PartitionListMessage> aFMatrixDimPartition(
        net.KeyValueListMessage request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_AFMATRIX_DIM_PARTITION, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<net.SLKVListMessage> getIndexOfSparseDim(
        net.SListMessage request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_GET_INDEX_OF_SPARSE_DIM, getCallOptions()), request);
    }
  }

  private static final int METHODID_PUSH_AFMATRIX = 0;
  private static final int METHODID_AFMATRIX_DIM_PARTITION = 1;
  private static final int METHODID_GET_INDEX_OF_SPARSE_DIM = 2;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PS serviceImpl;
    private final int methodId;

    public MethodHandlers(PS serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUSH_AFMATRIX:
          serviceImpl.pushAFMatrix((net.MatrixMessage) request,
              (io.grpc.stub.StreamObserver<net.MatrixMessage>) responseObserver);
          break;
        case METHODID_AFMATRIX_DIM_PARTITION:
          serviceImpl.aFMatrixDimPartition((net.KeyValueListMessage) request,
              (io.grpc.stub.StreamObserver<net.PartitionListMessage>) responseObserver);
          break;
        case METHODID_GET_INDEX_OF_SPARSE_DIM:
          serviceImpl.getIndexOfSparseDim((net.SListMessage) request,
              (io.grpc.stub.StreamObserver<net.SLKVListMessage>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final PS serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder(SERVICE_NAME)
        .addMethod(
          METHOD_PUSH_AFMATRIX,
          asyncUnaryCall(
            new MethodHandlers<
              net.MatrixMessage,
              net.MatrixMessage>(
                serviceImpl, METHODID_PUSH_AFMATRIX)))
        .addMethod(
          METHOD_AFMATRIX_DIM_PARTITION,
          asyncUnaryCall(
            new MethodHandlers<
              net.KeyValueListMessage,
              net.PartitionListMessage>(
                serviceImpl, METHODID_AFMATRIX_DIM_PARTITION)))
        .addMethod(
          METHOD_GET_INDEX_OF_SPARSE_DIM,
          asyncUnaryCall(
            new MethodHandlers<
              net.SListMessage,
              net.SLKVListMessage>(
                serviceImpl, METHODID_GET_INDEX_OF_SPARSE_DIM)))
        .build();
  }
}
