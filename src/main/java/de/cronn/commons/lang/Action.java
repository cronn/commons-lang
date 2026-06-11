package de.cronn.commons.lang;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Similar to {@link Runnable} but allows throwing checked exceptions.
 *
 * <p>Use this as a lambda-friendly handle for any void operation that may fail with a checked
 * exception, and convert it to standard JDK types via {@link #toSupplier()} or {@link
 * #toCallable()} where required.
 */
@FunctionalInterface
public interface Action {

  /** Executes this action. */
  void execute() throws Exception;

  /**
   * Adapts this action to a {@link Supplier}{@code <Void>}. Checked exceptions thrown by {@link
   * #execute()} are wrapped in a {@link RuntimeException}.
   */
  default Supplier<Void> toSupplier() {
    return () -> {
      try {
        execute();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    };
  }

  /**
   * Adapts this action to a {@link Callable}{@code <Void>}. Checked exceptions propagate unchanged.
   */
  default Callable<Void> toCallable() {
    return () -> {
      execute();
      return null;
    };
  }
}
