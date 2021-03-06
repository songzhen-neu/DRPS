// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ps.proto

package net;

/**
 * Protobuf type {@code net.SLKVMessage}
 */
public  final class SLKVMessage extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:net.SLKVMessage)
    SLKVMessageOrBuilder {
  // Use SLKVMessage.newBuilder() to construct.
  private SLKVMessage(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private SLKVMessage() {
    key_ = "";
    value_ = 0L;
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private SLKVMessage(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) {
    this();
    int mutable_bitField0_ = 0;
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!input.skipField(tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            key_ = s;
            break;
          }
          case 16: {

            value_ = input.readInt64();
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw new RuntimeException(e.setUnfinishedMessage(this));
    } catch (java.io.IOException e) {
      throw new RuntimeException(
          new com.google.protobuf.InvalidProtocolBufferException(
              e.getMessage()).setUnfinishedMessage(this));
    } finally {
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return net.Ps.internal_static_net_SLKVMessage_descriptor;
  }

  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return net.Ps.internal_static_net_SLKVMessage_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            net.SLKVMessage.class, net.SLKVMessage.Builder.class);
  }

  public static final int KEY_FIELD_NUMBER = 1;
  private volatile java.lang.Object key_;
  /**
   * <code>optional string key = 1;</code>
   */
  public java.lang.String getKey() {
    java.lang.Object ref = key_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      key_ = s;
      return s;
    }
  }
  /**
   * <code>optional string key = 1;</code>
   */
  public com.google.protobuf.ByteString
      getKeyBytes() {
    java.lang.Object ref = key_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      key_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int VALUE_FIELD_NUMBER = 2;
  private long value_;
  /**
   * <code>optional int64 value = 2;</code>
   */
  public long getValue() {
    return value_;
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!getKeyBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessage.writeString(output, 1, key_);
    }
    if (value_ != 0L) {
      output.writeInt64(2, value_);
    }
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getKeyBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessage.computeStringSize(1, key_);
    }
    if (value_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt64Size(2, value_);
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  public static net.SLKVMessage parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static net.SLKVMessage parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static net.SLKVMessage parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static net.SLKVMessage parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static net.SLKVMessage parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static net.SLKVMessage parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static net.SLKVMessage parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static net.SLKVMessage parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static net.SLKVMessage parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static net.SLKVMessage parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(net.SLKVMessage prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessage.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code net.SLKVMessage}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:net.SLKVMessage)
      net.SLKVMessageOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return net.Ps.internal_static_net_SLKVMessage_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return net.Ps.internal_static_net_SLKVMessage_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              net.SLKVMessage.class, net.SLKVMessage.Builder.class);
    }

    // Construct using net.SLKVMessage.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      key_ = "";

      value_ = 0L;

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return net.Ps.internal_static_net_SLKVMessage_descriptor;
    }

    public net.SLKVMessage getDefaultInstanceForType() {
      return net.SLKVMessage.getDefaultInstance();
    }

    public net.SLKVMessage build() {
      net.SLKVMessage result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public net.SLKVMessage buildPartial() {
      net.SLKVMessage result = new net.SLKVMessage(this);
      result.key_ = key_;
      result.value_ = value_;
      onBuilt();
      return result;
    }

    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof net.SLKVMessage) {
        return mergeFrom((net.SLKVMessage)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(net.SLKVMessage other) {
      if (other == net.SLKVMessage.getDefaultInstance()) return this;
      if (!other.getKey().isEmpty()) {
        key_ = other.key_;
        onChanged();
      }
      if (other.getValue() != 0L) {
        setValue(other.getValue());
      }
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      net.SLKVMessage parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (net.SLKVMessage) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object key_ = "";
    /**
     * <code>optional string key = 1;</code>
     */
    public java.lang.String getKey() {
      java.lang.Object ref = key_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        key_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>optional string key = 1;</code>
     */
    public com.google.protobuf.ByteString
        getKeyBytes() {
      java.lang.Object ref = key_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        key_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>optional string key = 1;</code>
     */
    public Builder setKey(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      key_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>optional string key = 1;</code>
     */
    public Builder clearKey() {
      
      key_ = getDefaultInstance().getKey();
      onChanged();
      return this;
    }
    /**
     * <code>optional string key = 1;</code>
     */
    public Builder setKeyBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      key_ = value;
      onChanged();
      return this;
    }

    private long value_ ;
    /**
     * <code>optional int64 value = 2;</code>
     */
    public long getValue() {
      return value_;
    }
    /**
     * <code>optional int64 value = 2;</code>
     */
    public Builder setValue(long value) {
      
      value_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>optional int64 value = 2;</code>
     */
    public Builder clearValue() {
      
      value_ = 0L;
      onChanged();
      return this;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }


    // @@protoc_insertion_point(builder_scope:net.SLKVMessage)
  }

  // @@protoc_insertion_point(class_scope:net.SLKVMessage)
  private static final net.SLKVMessage DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new net.SLKVMessage();
  }

  public static net.SLKVMessage getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<SLKVMessage>
      PARSER = new com.google.protobuf.AbstractParser<SLKVMessage>() {
    public SLKVMessage parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      try {
        return new SLKVMessage(input, extensionRegistry);
      } catch (RuntimeException e) {
        if (e.getCause() instanceof
            com.google.protobuf.InvalidProtocolBufferException) {
          throw (com.google.protobuf.InvalidProtocolBufferException)
              e.getCause();
        }
        throw e;
      }
    }
  };

  public static com.google.protobuf.Parser<SLKVMessage> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<SLKVMessage> getParserForType() {
    return PARSER;
  }

  public net.SLKVMessage getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

