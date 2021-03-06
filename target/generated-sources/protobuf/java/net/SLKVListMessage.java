// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ps.proto

package net;

/**
 * Protobuf type {@code net.SLKVListMessage}
 */
public  final class SLKVListMessage extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:net.SLKVListMessage)
    SLKVListMessageOrBuilder {
  // Use SLKVListMessage.newBuilder() to construct.
  private SLKVListMessage(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private SLKVListMessage() {
    size_ = 0;
    list_ = java.util.Collections.emptyList();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private SLKVListMessage(
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
          case 8: {

            size_ = input.readInt32();
            break;
          }
          case 18: {
            if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
              list_ = new java.util.ArrayList<net.SLKVMessage>();
              mutable_bitField0_ |= 0x00000002;
            }
            list_.add(input.readMessage(net.SLKVMessage.parser(), extensionRegistry));
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
      if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
        list_ = java.util.Collections.unmodifiableList(list_);
      }
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return net.Ps.internal_static_net_SLKVListMessage_descriptor;
  }

  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return net.Ps.internal_static_net_SLKVListMessage_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            net.SLKVListMessage.class, net.SLKVListMessage.Builder.class);
  }

  private int bitField0_;
  public static final int SIZE_FIELD_NUMBER = 1;
  private int size_;
  /**
   * <code>optional int32 size = 1;</code>
   */
  public int getSize() {
    return size_;
  }

  public static final int LIST_FIELD_NUMBER = 2;
  private java.util.List<net.SLKVMessage> list_;
  /**
   * <code>repeated .net.SLKVMessage list = 2;</code>
   */
  public java.util.List<net.SLKVMessage> getListList() {
    return list_;
  }
  /**
   * <code>repeated .net.SLKVMessage list = 2;</code>
   */
  public java.util.List<? extends net.SLKVMessageOrBuilder> 
      getListOrBuilderList() {
    return list_;
  }
  /**
   * <code>repeated .net.SLKVMessage list = 2;</code>
   */
  public int getListCount() {
    return list_.size();
  }
  /**
   * <code>repeated .net.SLKVMessage list = 2;</code>
   */
  public net.SLKVMessage getList(int index) {
    return list_.get(index);
  }
  /**
   * <code>repeated .net.SLKVMessage list = 2;</code>
   */
  public net.SLKVMessageOrBuilder getListOrBuilder(
      int index) {
    return list_.get(index);
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
    if (size_ != 0) {
      output.writeInt32(1, size_);
    }
    for (int i = 0; i < list_.size(); i++) {
      output.writeMessage(2, list_.get(i));
    }
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (size_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(1, size_);
    }
    for (int i = 0; i < list_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(2, list_.get(i));
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  public static net.SLKVListMessage parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static net.SLKVListMessage parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static net.SLKVListMessage parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static net.SLKVListMessage parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static net.SLKVListMessage parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static net.SLKVListMessage parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static net.SLKVListMessage parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static net.SLKVListMessage parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static net.SLKVListMessage parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static net.SLKVListMessage parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(net.SLKVListMessage prototype) {
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
   * Protobuf type {@code net.SLKVListMessage}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:net.SLKVListMessage)
      net.SLKVListMessageOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return net.Ps.internal_static_net_SLKVListMessage_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return net.Ps.internal_static_net_SLKVListMessage_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              net.SLKVListMessage.class, net.SLKVListMessage.Builder.class);
    }

    // Construct using net.SLKVListMessage.newBuilder()
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
        getListFieldBuilder();
      }
    }
    public Builder clear() {
      super.clear();
      size_ = 0;

      if (listBuilder_ == null) {
        list_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000002);
      } else {
        listBuilder_.clear();
      }
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return net.Ps.internal_static_net_SLKVListMessage_descriptor;
    }

    public net.SLKVListMessage getDefaultInstanceForType() {
      return net.SLKVListMessage.getDefaultInstance();
    }

    public net.SLKVListMessage build() {
      net.SLKVListMessage result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public net.SLKVListMessage buildPartial() {
      net.SLKVListMessage result = new net.SLKVListMessage(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      result.size_ = size_;
      if (listBuilder_ == null) {
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          list_ = java.util.Collections.unmodifiableList(list_);
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.list_ = list_;
      } else {
        result.list_ = listBuilder_.build();
      }
      result.bitField0_ = to_bitField0_;
      onBuilt();
      return result;
    }

    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof net.SLKVListMessage) {
        return mergeFrom((net.SLKVListMessage)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(net.SLKVListMessage other) {
      if (other == net.SLKVListMessage.getDefaultInstance()) return this;
      if (other.getSize() != 0) {
        setSize(other.getSize());
      }
      if (listBuilder_ == null) {
        if (!other.list_.isEmpty()) {
          if (list_.isEmpty()) {
            list_ = other.list_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureListIsMutable();
            list_.addAll(other.list_);
          }
          onChanged();
        }
      } else {
        if (!other.list_.isEmpty()) {
          if (listBuilder_.isEmpty()) {
            listBuilder_.dispose();
            listBuilder_ = null;
            list_ = other.list_;
            bitField0_ = (bitField0_ & ~0x00000002);
            listBuilder_ = 
              com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                 getListFieldBuilder() : null;
          } else {
            listBuilder_.addAllMessages(other.list_);
          }
        }
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
      net.SLKVListMessage parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (net.SLKVListMessage) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private int size_ ;
    /**
     * <code>optional int32 size = 1;</code>
     */
    public int getSize() {
      return size_;
    }
    /**
     * <code>optional int32 size = 1;</code>
     */
    public Builder setSize(int value) {
      
      size_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>optional int32 size = 1;</code>
     */
    public Builder clearSize() {
      
      size_ = 0;
      onChanged();
      return this;
    }

    private java.util.List<net.SLKVMessage> list_ =
      java.util.Collections.emptyList();
    private void ensureListIsMutable() {
      if (!((bitField0_ & 0x00000002) == 0x00000002)) {
        list_ = new java.util.ArrayList<net.SLKVMessage>(list_);
        bitField0_ |= 0x00000002;
       }
    }

    private com.google.protobuf.RepeatedFieldBuilder<
        net.SLKVMessage, net.SLKVMessage.Builder, net.SLKVMessageOrBuilder> listBuilder_;

    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public java.util.List<net.SLKVMessage> getListList() {
      if (listBuilder_ == null) {
        return java.util.Collections.unmodifiableList(list_);
      } else {
        return listBuilder_.getMessageList();
      }
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public int getListCount() {
      if (listBuilder_ == null) {
        return list_.size();
      } else {
        return listBuilder_.getCount();
      }
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public net.SLKVMessage getList(int index) {
      if (listBuilder_ == null) {
        return list_.get(index);
      } else {
        return listBuilder_.getMessage(index);
      }
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder setList(
        int index, net.SLKVMessage value) {
      if (listBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureListIsMutable();
        list_.set(index, value);
        onChanged();
      } else {
        listBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder setList(
        int index, net.SLKVMessage.Builder builderForValue) {
      if (listBuilder_ == null) {
        ensureListIsMutable();
        list_.set(index, builderForValue.build());
        onChanged();
      } else {
        listBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder addList(net.SLKVMessage value) {
      if (listBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureListIsMutable();
        list_.add(value);
        onChanged();
      } else {
        listBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder addList(
        int index, net.SLKVMessage value) {
      if (listBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureListIsMutable();
        list_.add(index, value);
        onChanged();
      } else {
        listBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder addList(
        net.SLKVMessage.Builder builderForValue) {
      if (listBuilder_ == null) {
        ensureListIsMutable();
        list_.add(builderForValue.build());
        onChanged();
      } else {
        listBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder addList(
        int index, net.SLKVMessage.Builder builderForValue) {
      if (listBuilder_ == null) {
        ensureListIsMutable();
        list_.add(index, builderForValue.build());
        onChanged();
      } else {
        listBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder addAllList(
        java.lang.Iterable<? extends net.SLKVMessage> values) {
      if (listBuilder_ == null) {
        ensureListIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, list_);
        onChanged();
      } else {
        listBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder clearList() {
      if (listBuilder_ == null) {
        list_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
      } else {
        listBuilder_.clear();
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public Builder removeList(int index) {
      if (listBuilder_ == null) {
        ensureListIsMutable();
        list_.remove(index);
        onChanged();
      } else {
        listBuilder_.remove(index);
      }
      return this;
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public net.SLKVMessage.Builder getListBuilder(
        int index) {
      return getListFieldBuilder().getBuilder(index);
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public net.SLKVMessageOrBuilder getListOrBuilder(
        int index) {
      if (listBuilder_ == null) {
        return list_.get(index);  } else {
        return listBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public java.util.List<? extends net.SLKVMessageOrBuilder> 
         getListOrBuilderList() {
      if (listBuilder_ != null) {
        return listBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(list_);
      }
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public net.SLKVMessage.Builder addListBuilder() {
      return getListFieldBuilder().addBuilder(
          net.SLKVMessage.getDefaultInstance());
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public net.SLKVMessage.Builder addListBuilder(
        int index) {
      return getListFieldBuilder().addBuilder(
          index, net.SLKVMessage.getDefaultInstance());
    }
    /**
     * <code>repeated .net.SLKVMessage list = 2;</code>
     */
    public java.util.List<net.SLKVMessage.Builder> 
         getListBuilderList() {
      return getListFieldBuilder().getBuilderList();
    }
    private com.google.protobuf.RepeatedFieldBuilder<
        net.SLKVMessage, net.SLKVMessage.Builder, net.SLKVMessageOrBuilder> 
        getListFieldBuilder() {
      if (listBuilder_ == null) {
        listBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
            net.SLKVMessage, net.SLKVMessage.Builder, net.SLKVMessageOrBuilder>(
                list_,
                ((bitField0_ & 0x00000002) == 0x00000002),
                getParentForChildren(),
                isClean());
        list_ = null;
      }
      return listBuilder_;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }


    // @@protoc_insertion_point(builder_scope:net.SLKVListMessage)
  }

  // @@protoc_insertion_point(class_scope:net.SLKVListMessage)
  private static final net.SLKVListMessage DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new net.SLKVListMessage();
  }

  public static net.SLKVListMessage getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<SLKVListMessage>
      PARSER = new com.google.protobuf.AbstractParser<SLKVListMessage>() {
    public SLKVListMessage parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      try {
        return new SLKVListMessage(input, extensionRegistry);
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

  public static com.google.protobuf.Parser<SLKVListMessage> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<SLKVListMessage> getParserForType() {
    return PARSER;
  }

  public net.SLKVListMessage getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

