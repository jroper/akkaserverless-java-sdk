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

import akka.testkit.EventFilter
import akka.testkit.SocketUtil
import com.akkaserverless.javasdk.AkkaServerless
import com.akkaserverless.javasdk.AkkaServerlessRunner
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object TestReplicatedEntity {
  def service(entityProvider: ReplicatedEntityProvider[_, _]): TestReplicatedService =
    new TestReplicatedService(entityProvider)
}

class TestReplicatedService(entityProvider: ReplicatedEntityProvider[_, _]) {
  val port: Int = SocketUtil.temporaryLocalPort()

  val config: Config = ConfigFactory.load(ConfigFactory.parseString(s"""
    akkaserverless {
      user-function-port = $port
      system.akka {
        loglevel = DEBUG
        loggers = ["akka.testkit.TestEventListener"]
        coordinated-shutdown.exit-jvm = off
      }
    }
  """))

  val runner: AkkaServerlessRunner = new AkkaServerless()
    .register(entityProvider)
    .createRunner(config)

  runner.run()

  def expectLogError[T](message: String)(block: => T): T =
    EventFilter.error(message, occurrences = 1).intercept(block)(runner.system)

  def terminate(): Unit = runner.terminate()
}
