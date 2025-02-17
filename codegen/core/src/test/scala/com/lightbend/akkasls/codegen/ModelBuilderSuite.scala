/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.akkasls.codegen

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors

import java.io.FileInputStream
import java.nio.file.Paths
import scala.jdk.CollectionConverters._
import scala.util.Using
import com.google.protobuf.ExtensionRegistry
import org.slf4j.LoggerFactory

import scala.collection.mutable

class ModelBuilderSuite extends munit.FunSuite {
  val log = LoggerFactory.getLogger(getClass)
  implicit val codegenLog = new Log {
    override def debug(message: String): Unit = log.debug(message)
    override def info(message: String): Unit = log.info(message)
  }

  def command(
      fqn: FullyQualifiedName,
      inputType: FullyQualifiedName,
      outputType: FullyQualifiedName,
      streamedInput: Boolean = false,
      streamedOutput: Boolean = false,
      inFromTopic: Boolean = false,
      outToTopic: Boolean = false) =
    ModelBuilder.Command(fqn, inputType, outputType, streamedInput, streamedOutput, inFromTopic, outToTopic)

  test("EventSourcedEntity introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/event-sourced-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors: mutable.Seq[Descriptors.FileDescriptor] =
        fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "google.protobuf",
          Some("google.golang.org/protobuf/types/known/emptypb"),
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val entity =
        ModelBuilder.EventSourcedEntity(
          FullyQualifiedName("ShoppingCart", domainProto),
          "eventsourced-shopping-cart",
          ModelBuilder.State(FullyQualifiedName("Cart", domainProto)),
          List(
            ModelBuilder.Event(FullyQualifiedName("ItemAdded", domainProto)),
            ModelBuilder.Event(FullyQualifiedName("ItemRemoved", domainProto))))

      assertEquals(model.entities, Map(entity.fqn.fullQualifiedName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            FullyQualifiedName("ShoppingCartService", shoppingCartProto),
            List(
              command(
                FullyQualifiedName("AddItem", shoppingCartProto),
                FullyQualifiedName("AddLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto)),
              command(
                FullyQualifiedName("RemoveItem", shoppingCartProto),
                FullyQualifiedName("RemoveLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto)),
              command(
                FullyQualifiedName("GetCart", shoppingCartProto),
                FullyQualifiedName("GetShoppingCart", shoppingCartProto),
                FullyQualifiedName("Cart", shoppingCartProto))),
            entity.fqn.fullQualifiedName)))
    }.get
  }

  test("ValueEntity introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/value-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "google.protobuf",
          Some("google.golang.org/protobuf/types/known/emptypb"),
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)
      val entity = ModelBuilder.ValueEntity(
        FullyQualifiedName("ShoppingCart", domainProto),
        "shopping-cart",
        ModelBuilder.State(FullyQualifiedName("Cart", domainProto)))

      assertEquals(model.entities, Map(entity.fqn.fullQualifiedName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            FullyQualifiedName("ShoppingCartService", shoppingCartProto),
            List(
              command(
                FullyQualifiedName("AddItem", shoppingCartProto),
                FullyQualifiedName("AddLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto)),
              command(
                FullyQualifiedName("RemoveItem", shoppingCartProto),
                FullyQualifiedName("RemoveLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto)),
              command(
                FullyQualifiedName("GetCart", shoppingCartProto),
                FullyQualifiedName("GetShoppingCart", shoppingCartProto),
                FullyQualifiedName("Cart", shoppingCartProto)),
              command(
                FullyQualifiedName("RemoveCart", shoppingCartProto),
                FullyQualifiedName("RemoveShoppingCart", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto))),
            entity.fqn.fullQualifiedName)))
    }.get
  }

  test("ReplicatedEntity introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/replicated-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "google.protobuf",
          Some("google.golang.org/protobuf/types/known/emptypb"),
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val entity = ModelBuilder.ReplicatedEntity(
        FullyQualifiedName("ShoppingCart", domainProto),
        "shopping-cart",
        ModelBuilder.ReplicatedCounterMap(ModelBuilder.TypeArgument("Product", domainProto)))

      assertEquals(model.entities, Map(entity.fqn.fullQualifiedName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            FullyQualifiedName("ShoppingCartService", shoppingCartProto),
            List(
              command(
                FullyQualifiedName("AddItem", shoppingCartProto),
                FullyQualifiedName("AddLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto)),
              command(
                FullyQualifiedName("RemoveItem", shoppingCartProto),
                FullyQualifiedName("RemoveLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto)),
              command(
                FullyQualifiedName("GetCart", shoppingCartProto),
                FullyQualifiedName("GetShoppingCart", shoppingCartProto),
                FullyQualifiedName("Cart", shoppingCartProto)),
              command(
                FullyQualifiedName("RemoveCart", shoppingCartProto),
                FullyQualifiedName("RemoveShoppingCart", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto))),
            entity.fqn.fullQualifiedName)))
    }.get
  }

  test("View introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/view-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)
    registry.add(com.akkaserverless.Annotations.method)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.foldLeft(List.empty[Descriptors.FileDescriptor])((acc, file) =>
        acc :+ Descriptors.FileDescriptor.buildFrom(file, acc.toArray, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "ShoppingCartViewModel",
          "shopping.cart.view",
          None,
          None,
          Some("ShoppingCartViewModel"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "ShoppingCartDomain",
          "shopping.cart.domain",
          None,
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "google.protobuf",
          Some("google.golang.org/protobuf/types/known/emptypb"),
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)
      val entity = ModelBuilder.ValueEntity(
        FullyQualifiedName("ShoppingCart", domainProto),
        "shopping-cart",
        ModelBuilder.State(FullyQualifiedName("Cart", domainProto)))

      val transformedUpdates =
        List(
          command(
            FullyQualifiedName("ProcessAdded", shoppingCartProto),
            FullyQualifiedName("ItemAdded", domainProto),
            FullyQualifiedName("CartViewState", shoppingCartProto)),
          command(
            FullyQualifiedName("ProcessRemoved", shoppingCartProto),
            FullyQualifiedName("ItemRemoved", domainProto),
            FullyQualifiedName("CartViewState", shoppingCartProto)),
          command(
            FullyQualifiedName("ProcessCheckedOut", shoppingCartProto),
            FullyQualifiedName("CheckedOut", domainProto),
            FullyQualifiedName("CartViewState", shoppingCartProto)))
      val queries = List(
        command(
          FullyQualifiedName("GetCheckedOutCarts", shoppingCartProto),
          FullyQualifiedName("GetCheckedOutCartsRequest", shoppingCartProto),
          FullyQualifiedName("CartViewState", shoppingCartProto),
          streamedOutput = true))
      assertEquals(
        model.services,
        Map(
          "shopping.cart.view.ShoppingCartViewServiceView" ->
          ModelBuilder.ViewService(
            FullyQualifiedName("ShoppingCartViewService", "ShoppingCartViewServiceView", shoppingCartProto),
            transformedUpdates ++ queries,
            "ShoppingCartViewService",
            transformedUpdates,
            transformedUpdates)))
    }.get
  }

  test("deriving java package from proto options") {
    val name = "Name"
    val pkg = "com.example"

    assertEquals(PackageNaming(name, pkg, None, None, None, javaMultipleFiles = false).javaPackage, pkg)
    assertEquals(
      PackageNaming(name, pkg, None, Some("override.package"), None, javaMultipleFiles = false).javaPackage,
      "override.package")
  }

  test("resolving full names") {
    val pkg = "com.example"

    assertEquals(ModelBuilder.resolveFullName("Test", pkg), "com.example.Test")
    assertEquals(ModelBuilder.resolveFullName(".sub.Test", pkg), "com.example.sub.Test")
    assertEquals(ModelBuilder.resolveFullName("other.package.Test", pkg), "other.package.Test")
  }
}
