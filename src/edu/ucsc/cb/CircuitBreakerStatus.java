package edu.ucsc.cb;

/**
 * the {@link CircuitBreaker breaker}'s status choices.
 *
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public enum CircuitBreakerStatus {
  /**
   * the {@link CircuitBreaker breaker} is not taking any new calls.
   */
  OPENED,

  /**
   * the {@link CircuitBreaker breaker} is now taking any new calls.
   */
  CLOSED,

  /**
   * after certain time has passed, the {@link CircuitBreaker breaker} is willing to let one
   * call go. If it fails, then the {@link CircuitBreaker breaker} will not allow any calls
   * to go through (i.e., {@link #OPENED}).
   */
  HALF_OPENED
}
