package edu.ucsc.cb;

/**
 * Keywords known by the {@link CircuitBreakerEnvironment} to load data from a
 * circuit breaker's configuration file.
 *
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public class CircuitBreakerEnvironmentProperties {
  /**
   * Name of the configuration properties file.
   */
  public static final String CONFIG_FILE = System.getProperty("user.dir") + "/config/circuitbreaker.cfg";

  /**
   * How long the client should wait before trying again (in nanoseconds)
   */
  public static final String RETRY_THRESHOLD            = "circuit.breaker.retry.threshold.ns";

  /**
   * How many errors the breaker will swallow before caching the
   * error and consequently force the client to wait.
   */
  public static final String FAILED_CALL_THRESHOLD      = "circuit.breaker.failed.call.threshold";

  /**
   * Force the breaker to display its activity messages on screen
   */
  public static final String DEBUG = "circuit.breaker.debug";

  /**
   * Never invoked
   */
  private CircuitBreakerEnvironmentProperties(){
    throw new AssertionError("Error! This class is a utility class");
  }
}
