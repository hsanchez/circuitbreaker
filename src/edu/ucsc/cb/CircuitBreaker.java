package edu.ucsc.cb;

/**
 * Represents the {@link CircuitBreaker} type.
 *
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public interface CircuitBreaker <E extends Exception> {
  /**
   * breaker acknowledges the intended call.
   * @throws E
   *    thrown if the breaker is not ready to accept new calls.
   */
  void callStarted() throws E;

  /**
   * breaker changes its state to {@link CircuitBreakerStatus#CLOSED}
   */
  void callSucceeded();

  /**
   * cb remains or changes its state to {@link CircuitBreakerStatus#OPENED}
   * @param error
   *      error that opened circuit breaker.
   *
   */
  void callFailed(E error);

  /**
   * @return
   *    The amount of time in milliseconds before the circuit breaker will let calls
   *    to go through.
   */
  long getRetryThreshold();

  /**
   * @return
   *    The number of calls that must fail before the circuit breaker is opened.
   */
  long getFailedCallThreshold();

  /**
   * @return
   *    basic statistics of circuit breaker's execution.
   */
  CircuitBreakerStatistics getStatistics();

  /**
   * @return
   *      the status of the breaker.
   */
  CircuitBreakerStatus status();
}
