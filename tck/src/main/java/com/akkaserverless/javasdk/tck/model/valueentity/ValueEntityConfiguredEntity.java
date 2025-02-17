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

package com.akkaserverless.javasdk.tck.model.valueentity;

import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.akkaserverless.tck.model.ValueEntity.Request;
import com.akkaserverless.tck.model.ValueEntity.Response;

public class ValueEntityConfiguredEntity extends ValueEntity<String> {

  public ValueEntityConfiguredEntity(ValueEntityContext context) {}

  public Effect<Response> call(String state, Request request) {
    return effects().reply(Response.getDefaultInstance());
  }

  @Override
  public String emptyState() {
    return null;
  }
}
