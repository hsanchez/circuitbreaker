package edu.ucsc.cb;

import java.util.concurrent.Callable;

/**
 * ...
 *
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public class CircuitBreakers {
  private CircuitBreakers() {
    throw new AssertionError("Error! This is a utility class.");
  }

  public static <T> T safeguard(Callable<T> block, String message,
      CircuitBreaker<RuntimeException> breaker){
    //noinspection RedundantTypeArguments
    return CircuitBreakers.<T, RuntimeException>circuitBreak(block, message, breaker);
  }

  public static <T, E extends Exception> T circuitBreak(Callable<T> block, String message,
      CircuitBreaker<E> breaker) throws E {
    breaker.callStarted();
    try {
      final T result = block.call();
      breaker.callSucceeded();
      return result;
    } catch (Exception cause) {
      System.err.println(message);
      @SuppressWarnings({"RedundantTypeArguments"})
      final E exception = Exceptions.<E>castException(cause);
      breaker.callFailed(exception);
      throw exception;
    }
  }

  /**
   * create a new circuit breaker. this breaker relies on a configuration file located under
   * <project_directory>/config/circuitbreaker.cfg.
   *
   * @param <E> cached exception
   * @return a circuit breaker which caches {@literal E} exceptions.
   */
  public static <E extends Exception> CircuitBreaker<E> newCircuitBreaker() {
    return new DefaultCircuitBreaker<E>();
  }

  /**
   *
   * @param failedCallThreshold the number of errors the breaker will swallow before caching the
   * error and consequently force the client to wait.
   * @param retryThreshold how long the circuit breaker will wait until letting the call go thru.
   * @param debug {@code true} if the breaker will display its activity messages. {@code false} otherwise.
   * @return a circuit breaker which caches {@literal E} exceptions.
   */
  public static <E extends Exception> CircuitBreaker<E> newCircuitBreaker(long failedCallThreshold,
      long retryThreshold, boolean debug) {
    return new DefaultCircuitBreaker<E>(failedCallThreshold, retryThreshold, debug);
  }

  public static void main(String[] args) {
    final CircuitBreaker<RuntimeException> breaker = newCircuitBreaker(3000, 1L, true);
    final Callable<String> call = new Callable<String>() {
      int count = 1;
      @Override public String call() throws Exception {
        count++;
        if(count == 3) {
          count = 0;
          throw new Exception("Reach limit");
        }
        return "One, two, three, four, etc.";
      }
    };
    for(int idx = 0; idx < 11; idx++){ // attempts to access service
      try {
        System.out.println(safeguard(call, "Error has occurred!", breaker));
      } catch (Exception e){
        System.out.println("error at" + idx + " attempt.");
      }
    }
  }
}
