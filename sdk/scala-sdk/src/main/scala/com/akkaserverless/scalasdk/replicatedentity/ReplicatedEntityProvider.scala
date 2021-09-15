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

package com.akkaserverless.scalasdk.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedEntityProvider => Impl }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedData => DataImpl }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedEntity => EntityImpl }
//FIXME just placeholders for now

trait ReplicatedData extends DataImpl

abstract class ReplicatedEntity[D <: ReplicatedData] extends EntityImpl[D]

class ReplicatedEntityProvider[D <: ReplicatedData, E <: ReplicatedEntity[D]](
    private[akkaserverless] val impl: Impl[D, E])
