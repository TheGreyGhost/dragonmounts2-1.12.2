package com.TheRPGAdventurer.ROTD.util;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by TGG on 8/11/2019.
 * A simple utility to count the number of ticks up until a maximum.  When the timer reaches maximum, the
 *   count either stays at the maximum value, or is reset to "Expired", depending on chosen behaviour
 *
 * Usage:
 * 1) Create TickTimer with optional expiry time and reset behaviour
 * 2) every tick, call tick()
 * 3) When the timer should be started, call restart()
 * 4) check the tick count using isExpired() and/or getTickCount()
 *
 */
public class TickTimer {

  public TickTimer() {
    this(10000, true);
  }

  /**
   * Custom timer which counts up to the given maximum and then resets itself to zero and stops
   * @param expiryTime
   * @param reset
   */
  public TickTimer(int expiryTime, boolean reset) {
    checkArgument(expiryTime > 0);
    TICK_RESET_TIME = expiryTime;
    this.reset = reset;
  }

  /**
   * Starts the timer running
   */
  public void restart() {
    tickCount = 0;
  }

  public Optional<Integer> getTickCount() {
    return (tickCount == TIMER_EXPIRED_VALUE) ? Optional.empty() : Optional.of(tickCount);
  }

  public boolean isExpired() {
    return tickCount == TIMER_EXPIRED_VALUE;
  }

  public void tick() {
   if (tickCount != TIMER_EXPIRED_VALUE && tickCount < TICK_RESET_TIME) {
      ++tickCount;
    }
    if (tickCount >= TICK_RESET_TIME && reset) {
      tickCount = TIMER_EXPIRED_VALUE;
    }
  }

  private final static int TIMER_EXPIRED_VALUE = -1;
  private final int TICK_RESET_TIME;
  private final boolean reset;
  private int tickCount;
}
