// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ps.proto

package net;

public interface SListMessageOrBuilder extends
    // @@protoc_insertion_point(interface_extends:net.SListMessage)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional int32 size = 1;</code>
   */
  int getSize();

  /**
   * <code>repeated string list = 2;</code>
   */
  com.google.protobuf.ProtocolStringList
      getListList();
  /**
   * <code>repeated string list = 2;</code>
   */
  int getListCount();
  /**
   * <code>repeated string list = 2;</code>
   */
  java.lang.String getList(int index);
  /**
   * <code>repeated string list = 2;</code>
   */
  com.google.protobuf.ByteString
      getListBytes(int index);
}