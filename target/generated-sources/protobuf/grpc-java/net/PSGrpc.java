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
  public static final io.grpc.MethodDescriptor<net.HelloRequest,
      net.HelloReply> METHOD_SAY_HELLO =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "net.PS", "SayHello"),
          io.grpc.protobuf.ProtoUtils.marshaller(net.HelloRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(net.HelloReply.getDefaultInstance()));

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
    public void sayHello(net.HelloRequest request,
        io.grpc.stub.StreamObserver<net.HelloReply> responseObserver);
  }

  @io.grpc.ExperimentalApi
  public static abstract class AbstractPS implements PS, io.grpc.BindableService {

    @java.lang.Override
    public void sayHello(net.HelloRequest request,
        io.grpc.stub.StreamObserver<net.HelloReply> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_SAY_HELLO, responseObserver);
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
    public net.HelloReply sayHello(net.HelloRequest request);
  }

  /**
   */
  public static interface PSFutureClient {

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<net.HelloReply> sayHello(
        net.HelloRequest request);
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
    public void sayHello(net.HelloRequest request,
        io.grpc.stub.StreamObserver<net.HelloReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_SAY_HELLO, getCallOptions()), request, responseObserver);
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
    public net.HelloReply sayHello(net.HelloRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_SAY_HELLO, getCallOptions(), request);
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
    public com.google.common.util.concurrent.ListenableFuture<net.HelloReply> sayHello(
        net.HelloRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_SAY_HELLO, getCallOptions()), request);
    }
  }

  private static final int METHODID_SAY_HELLO = 0;

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
        case METHODID_SAY_HELLO:
          serviceImpl.sayHello((net.HelloRequest) request,
              (io.grpc.stub.StreamObserver<net.HelloReply>) responseObserver);
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
          METHOD_SAY_HELLO,
          asyncUnaryCall(
            new MethodHandlers<
              net.HelloRequest,
              net.HelloReply>(
                serviceImpl, METHODID_SAY_HELLO)))
        .build();
  }
}
