package edu.ucsc.cb;

import static edu.ucsc.cb.CircuitBreakers.newCircuitBreaker;
import static edu.ucsc.cb.CircuitBreakers.safeguard;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.Test;

/**
 * ...
 *
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public class CircuitBreakerTest {
  @Test public void testCircuitBreaker() throws Exception {
    final CircuitBreaker<RuntimeException> breaker = newCircuitBreaker(3000, 1L, true);
    final AtomicInteger expectedBreaks = new AtomicInteger();
    final Callable<String> call = new Callable<String>() {
      int count = 1;

      @Override public String call() throws Exception {
        count++;
        if (count % 3 == 0) {
          count = 0;
          expectedBreaks.incrementAndGet();
          throw new Exception("Reached limit");
        }
        return "One, two, three, four, etc.";
      }
    };

    for (int idx = 0; idx < 120; idx++) { // attempts to access service
      try {
        System.out.println(safeguard(call, "Error has occurred!", breaker));
      } catch (Exception e) {
        System.out.println("error at" + idx + " attempt.");
      }
    }

    assertThat(expectedBreaks.get(), equalTo(40));
  }
}
