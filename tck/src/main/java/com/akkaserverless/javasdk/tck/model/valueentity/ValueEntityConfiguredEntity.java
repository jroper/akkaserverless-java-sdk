/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.valueentity;

import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.tck.model.ValueEntity.Request;
import com.akkaserverless.tck.model.ValueEntity.Response;

@ValueEntity(entityType = "value-entity-configured")
public class ValueEntityConfiguredEntity {

  @CommandHandler
  public Response call(Request request) {
    return Response.getDefaultInstance();
  }
}
