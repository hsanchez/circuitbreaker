package edu.ucsc.cb;

import java.lang.reflect.Constructor;

/**
 * A convenient class for dealing with generic Exceptions.i.e., those ones
 * product of using <E extends Exception> or <E extends Throwable> and want to
 * do
 * <p>
 *   throw new E
 * </p>
 *
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public class Exceptions {
  private Exceptions(){
    throw new AssertionError("Error! This is a utility class.");
  }

  public static <T extends Exception> T throwException(Exception that) throws T {
    try {
      //we are casting from some subclass of exception to exception --- bad bad
      @SuppressWarnings({"RedundantCast"})
      final Constructor<T> c = newException();
      throw c.newInstance(that);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static <T extends Exception> Constructor<T> newException() throws T {
    try {
      //we are casting from some subclass of exception to exception --- bad bad
      //noinspection RedundantCast
      return (Constructor<T>) Exception.class.getDeclaredConstructor(Throwable.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T extends Exception> T castException(Exception that) throws T {
    try {
      //we are casting from some subclass of exception to exception --- bad bad
      @SuppressWarnings({"RedundantCast"})
      final Constructor<T> c = newException();
      return c.newInstance(that);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
