/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

import com.akkaserverless.javasdk.impl.Timeout;

import java.time.Duration;

/** A passivation strategy. */
public interface PassivationStrategy {

  /**
   * Create a passivation strategy that passivates the entity after the default duration (30
   * seconds) of inactivity.
   *
   * @return the passivation strategy
   */
  static PassivationStrategy defaultTimeout() {
    return new Timeout();
  }

  /**
   * Create a passivation strategy that passivates the entity after a given duration of inactivity.
   *
   * @param duration of inactivity after which the passivation should occur.
   * @return the passivation strategy
   */
  static PassivationStrategy timeout(Duration duration) {
    return new Timeout(duration);
  }
}
