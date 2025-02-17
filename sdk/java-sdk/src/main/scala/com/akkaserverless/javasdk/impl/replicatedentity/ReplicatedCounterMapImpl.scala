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

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMap
import com.akkaserverless.protocol.replicated_entity.{
  ReplicatedCounterMapDelta,
  ReplicatedCounterMapEntryDelta,
  ReplicatedEntityDelta
}

import scala.jdk.CollectionConverters._

private[replicatedentity] final class ReplicatedCounterMapImpl[K](
    anySupport: AnySupport,
    counters: Map[K, ReplicatedCounterImpl] = Map.empty[K, ReplicatedCounterImpl],
    removed: Set[K] = Set.empty[K],
    cleared: Boolean = false)
    extends ReplicatedCounterMap[K]
    with InternalReplicatedData {

  override type Self = ReplicatedCounterMapImpl[K]
  override val name = "ReplicatedCounterMap"

  override def get(key: K): Long = counters.get(key).fold(0L)(_.getValue)

  override def increment(key: K, amount: Long): ReplicatedCounterMapImpl[K] = {
    val counter = counters.getOrElse(key, new ReplicatedCounterImpl)
    val incremented = counter.increment(amount)
    new ReplicatedCounterMapImpl(anySupport, counters.updated(key, incremented), removed, cleared)
  }

  override def decrement(key: K, amount: Long): ReplicatedCounterMapImpl[K] = increment(key, -amount)

  override def remove(key: K): ReplicatedCounterMapImpl[K] = {
    if (!counters.contains(key)) {
      this
    } else {
      new ReplicatedCounterMapImpl(anySupport, counters.removed(key), removed + key, cleared)
    }
  }

  override def clear(): ReplicatedCounterMapImpl[K] =
    new ReplicatedCounterMapImpl[K](anySupport, cleared = true)

  override def size: Int = counters.size

  override def isEmpty: Boolean = counters.isEmpty

  override def containsKey(key: K): Boolean = counters.contains(key)

  override def keySet: java.util.Set[K] = counters.keySet.asJava

  override def hasDelta: Boolean = cleared || removed.nonEmpty || counters.values.exists(_.hasDelta)

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.ReplicatedCounterMap(
      ReplicatedCounterMapDelta(
        cleared = cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        updated = counters.collect {
          case (key, counter) if counter.hasDelta =>
            ReplicatedCounterMapEntryDelta(Some(anySupport.encodeScala(key)), counter.getDelta.counter)
        }.toSeq))

  override def resetDelta(): ReplicatedCounterMapImpl[K] =
    if (hasDelta) new ReplicatedCounterMapImpl(anySupport, counters.view.mapValues(_.resetDelta()).toMap) else this

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedCounterMapImpl[K]] = {
    case ReplicatedEntityDelta.Delta.ReplicatedCounterMap(ReplicatedCounterMapDelta(cleared, removed, updated, _)) =>
      val reducedCounters =
        if (cleared) Map.empty[K, ReplicatedCounterImpl]
        else counters -- removed.map(key => anySupport.decode(key).asInstanceOf[K])
      val updatedCounters = updated.foldLeft(reducedCounters) {
        case (map, ReplicatedCounterMapEntryDelta(Some(encodedKey), Some(delta), _)) =>
          val key = anySupport.decode(encodedKey).asInstanceOf[K]
          val counter = map.getOrElse(key, new ReplicatedCounterImpl)
          map.updated(key, counter.applyDelta(ReplicatedEntityDelta.Delta.Counter(delta)))
        case (map, _) => map
      }
      new ReplicatedCounterMapImpl(anySupport, updatedCounters)
  }

  override def toString = s"ReplicatedCounterMap(${counters.map { case (k, v) => s"$k->$v" }.mkString(",")})"
}
