package de.cronn.commons.lang;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/** Similar to {@link Runnable} but it allows to throw checked exceptions. */
@FunctionalInterface
public interface Action {

  void execute() throws Exception;

  default Supplier<Void> toSupplier() {
    return () -> {
      try {
        execute();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    };
  }

  default Callable<Void> toCallable() {
    return () -> {
      execute();
      return null;
    };
  }
}
