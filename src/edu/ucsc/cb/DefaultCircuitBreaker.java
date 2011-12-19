package edu.ucsc.cb;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * basic circuit breaker. the breaker will wait some 'time' before letting calls go thru.
 *
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public class DefaultCircuitBreaker<E extends Exception> implements CircuitBreaker<E> {
  private static final CircuitBreakerEnvironment ENV = CircuitBreakerEnvironment.getInstance();

  private static final long FAILED_CALL_THRESHOLD = ENV.getCircuitBreakerFailedCallThreshold();//1L;
  private static final long RETRY_THRESHOLD       = ENV.getCircuitBreakerRetryThreshold(); //15 * 60 * 1000;
  private static final long NO_CLOSED_YET         = -1L;
  private static final boolean DEBUG              = ENV.inDebugMode();

  private AtomicLong failedCalls   = new AtomicLong(); // # of failed calls since the cb closed.
  private AtomicLong blockedCalls  = new AtomicLong(); // # of calls blocked by the cb.

  private AtomicReference<CircuitBreakerStatus> status  = new AtomicReference<CircuitBreakerStatus>(
      CircuitBreakerStatus.CLOSED);  // cb's current status
  private AtomicReference<E>      lastException   = new AtomicReference<E>();                    // last registered exception if the cb's status == Open. Otherwise, it should be NULL.
  private AtomicLong              nextTryTime     = new AtomicLong();                            // time at which the circuit breaker may retry letting an operation to be called.
  private AtomicLong              avgRecoveryTime = new AtomicLong();                            // time at which the cb was able to reset itself.

  private final long failedCallThreshold;
  private final long retryThreshold;
  private final boolean debug;

  public DefaultCircuitBreaker(){
    this(FAILED_CALL_THRESHOLD, RETRY_THRESHOLD, DEBUG);
  }

  public DefaultCircuitBreaker(long failedCallThreshold, long retryThreshold, boolean debug){
    this.failedCallThreshold  = failedCallThreshold;
    this.retryThreshold       = retryThreshold;
    this.debug                = debug;
  }

  @Override public void callStarted() throws E {
    if (status() == CircuitBreakerStatus.OPENED) {
      final long currentNextTryTime         = nextTryTime.get();
      final long currentTime                = System.nanoTime();

      if (currentTime < currentNextTryTime){
        blockedCalls.incrementAndGet();
        //noinspection ThrowableResultOfMethodCallIgnored
        Exceptions
            .<E>throwException(lastException.get());
      } else {
        if(status.compareAndSet(CircuitBreakerStatus.OPENED, CircuitBreakerStatus.HALF_OPENED)){
            avgRecoveryTime.set(currentTime - currentNextTryTime);
            notifyStatusChange(CircuitBreakerStatus.OPENED, CircuitBreakerStatus.HALF_OPENED);
        }
      }
    }
  }

  @Override public void callSucceeded() {
    reset();
  }

  @Override public void callFailed(E error) {
    if(CircuitBreakerStatus.HALF_OPENED == status()){
      if(status.compareAndSet(CircuitBreakerStatus.HALF_OPENED, CircuitBreakerStatus.OPENED)){
          notifyStatusChange(CircuitBreakerStatus.HALF_OPENED, CircuitBreakerStatus.OPENED);
      }

      nextTryTime.set(System.nanoTime() + getRetryThreshold());
      lastException.compareAndSet(null, error);

    } else if(CircuitBreakerStatus.CLOSED == status()){
      final long failed = failedCalls.incrementAndGet();
      if (failed >= getFailedCallThreshold()) {
          if (status.compareAndSet(CircuitBreakerStatus.CLOSED, CircuitBreakerStatus.OPENED)) {
              notifyStatusChange(CircuitBreakerStatus.CLOSED, CircuitBreakerStatus.OPENED);
          }

          nextTryTime.set(System.nanoTime() + getRetryThreshold());
          lastException.compareAndSet(null, error);

      }
    }
  }

  @Override public long getRetryThreshold() {
    return retryThreshold;
  }

  @Override public long getFailedCallThreshold() {
    return failedCallThreshold;
  }

  @Override public CircuitBreakerStatistics getStatistics() {
    return new BasicCircuitBreakerStatistics(
        status() == CircuitBreakerStatus.CLOSED
            ? avgRecoveryTime.get()
            : NO_CLOSED_YET
    );
  }

  /**
   * notify whichever application (i.e., logging) that needs an update from
   * the circuit breaker.
   * @param oldStatus
   *          old status
   * @param newStatus
   *          new status
   */
  void notifyStatusChange(CircuitBreakerStatus oldStatus, CircuitBreakerStatus newStatus) {
    if((oldStatus == newStatus) && (newStatus == CircuitBreakerStatus.CLOSED)) return;
    if(!debug) return;

    System.out.println("Circuit breaker's status has changed from "
        + oldStatus + " to " + newStatus
        + ((CircuitBreakerStatus.CLOSED != newStatus)
        ? ". Next cooling-down time is set to: "
        + nextTryTime.get() + " nanoseconds. Current blocked calls: "
        + blockedCalls.get()
        : "") + ". Average recovery time: "
        + getStatistics().getAvgRecoveryTime() + " nanoseconds."
    );
  }

  /**
   *  reset the circuit breaker.
   */
  private void reset() {
    notifyStatusChange(status(), CircuitBreakerStatus.CLOSED);
    status.set(CircuitBreakerStatus.CLOSED);
    failedCalls.set(0L);
    blockedCalls.set(0L);
    lastException.set(null);
  }

  @Override public CircuitBreakerStatus status() {
    return status.get();
  }

  /**
   * object that holds the statistics for
   */
  private static class BasicCircuitBreakerStatistics implements CircuitBreakerStatistics {
    private final long avgRecoveryTime;

    BasicCircuitBreakerStatistics(long avgRecoveryTime){
      this.avgRecoveryTime = avgRecoveryTime;
    }

    @Override public long getAvgRecoveryTime() {
      return avgRecoveryTime;
    }
  }
}
