package org.gusdb.fgputil.functional;

import static org.gusdb.fgputil.iterator.IteratorUtil.toIterable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.functional.FunctionalInterfaces.BiFunctionWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.FunctionWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.PredicateWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.Reducer;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ReducerWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.TriFunction;

public class Functions {

  private Functions() {}

  /**
   * Returns a function that takes a parameter of the given type and returns a string
   * @param <T> type to convert to string
   */
  public static class ToStringFunction<T> implements Function<T,String> {
    @Override
    public String apply(T obj) {
      return obj.toString();
    }
  }

  /**
   * Returns a copy (a new HashMap) of the input map with entries trimmed out whose keys do not pass the
   * passed predicate
   *
   * @param inputMap a map
   * @param keyPred a filter function on the keys of the map
   * @return a copy of the input map with non-passing entries removed
   */
  public static <S,T> Map<S,T> pickKeys(Map<S,T> inputMap, Predicate<S> keyPred) {
    return pickKeys(inputMap, keyPred, new LinkedHashMap<>());
  }

  /**
   * Adds any entries in the input map whose keys pass the predicate to the target map.  Performs the same
   * basic operation as the two-argument version but allows the caller to specify the target map
   * implementation (e.g. LinkedHashMap), or a non-empty target map if desired.
   *
   * @param inputMap a map of values
   * @param keyPred a filter function on the keys of the map
   * @param target a map into which the unfiltered elements should be placed
   * @return the target map
   */
  public static <S,T> Map<S,T> pickKeys(Map<S,T> inputMap, Predicate<S> keyPred, Map<S,T> target) {
    for (Entry<S,T> entry : inputMap.entrySet()) {
      if (keyPred.test(entry.getKey())) {
        target.put(entry.getKey(), entry.getValue());
      }
    }
    return target;
  }

  /**
   * Returns a copy (a new HashMap) of the input map with entries trimmed out whose values do not pass the
   * passed predicate
   *
   * @param inputMap a map
   * @param valuePred a filter function on the values of the map
   * @return a copy of the input map with non-passing entries removed
   */
  public static <S,T> Map<S,T> pickValues(Map<S,T> inputMap, Predicate<T> valuePred) {
    return pickValues(inputMap, valuePred, new HashMap<>());
  }

  /**
   * Adds any entries in the input map whose values pass the predicate to the target map.  Performs the same
   * basic operation as the two-argument version but allows the caller to specify the target map
   * implementation (e.g. LinkedHashMap), or a non-empty target map if desired.
   *
   * @param inputMap a map of values
   * @param valuePred a filter function on the values of the map
   * @param target a map into which the unfiltered elements should be placed
   * @return the target map
   */
  public static <S,T> Map<S,T> pickValues(Map<S,T> inputMap, Predicate<T> valuePred, Map<S,T> target) {
    for (Entry<S,T> entry : inputMap.entrySet()) {
      if (valuePred.test(entry.getValue())) {
        target.put(entry.getKey(), entry.getValue());
      }
    }
    return target;
  }

  /**
   * Converts an iterable of keys into a map from the key to a value generated by the passed function
   *
   * @param keys input keys
   * @param function value generator
   * @return map from passed keys to generated values
   */
  public static <S,T> Map<S,T> getMapFromKeys(Iterable<S> keys, Function<S,T> function) {
    return getMapFromList(keys, key -> new TwoTuple<>(key, function.apply(key)));
  }

  /**
   * Converts an iterable of values into a map where the key is a value generated by the passed function
   *
   * @param values input values
   * @param function key generator
   * @return map from generated keys to passed values
   */
  public static <R,S,T extends R> Map<S,T> getMapFromValues(Iterable<T> values, Function<R,S> function) {
    return getMapFromList(values, value -> new TwoTuple<>(function.apply(value),
        value));
  }

  /**
   * Converts an iterable of values into a map.  A passed function takes each value in the iterable and
   * transforms it to an entry for insertion into the returned map
   *
   * @param values input values
   * @param function entry generator
   * @return map containing the resulting entries
   */
  public static <R,S,T> Map<S,T> getMapFromList(Iterable<R> values, Function<R,Entry<S,T>> function) {
    return reduce(values,
        (acc, next) -> acc.put(next, function),
        new MapBuilder<S,T>(new LinkedHashMap<>()) // some callers depend on proper ordering
    ).toMap();
  }

  /**
   * Maps the given iterable elements to a List containing mapped elements.  The passed
   * function is executed on each input element; its outputs are placed in a new list and returned.
   *
   * @param inputs an iterable of input elements
   * @param function a function to be performed on each element
   * @return List of function outputs
   */
  public static <S,T,R extends S> List<T> mapToList(Iterable<R> inputs, Function<S,T> function) {
    List<T> result = new ArrayList<>();
    for (R obj : inputs) {
      result.add(function.apply(obj));
    }
    return result;
  }

  /**
   * Maps the given iterable elements to a List containing mapped elements.  The passed
   * function is executed on each input element and its iteration index; its outputs are
   * placed in a new list and returned.
   *
   * @param inputs an iterable of input elements
   * @param function a function to be performed on each element
   * @return List of function outputs
   */
  public static <S,T,R extends S> List<T> mapToListWithIndex(Iterable<R> inputs, BiFunction<S, Integer, T> function) {
    List<T> result = new ArrayList<>();
    int i = 0;
    for (R obj : inputs) {
      result.add(function.apply(obj, i++));
    }
    return result;
  }

  /**
   * Using the iterable collection and the predicate function passed in, filters those
   * items that satisfy the predicate into a new list
   *
   * @param inputs - iterable collection to filter
   * @param predicate - predicate function supplying the filter test
   * @return - new list of filtered items
   */
  public static <T> List<T> filter(Iterable<T> inputs, Predicate<T> predicate) {
    List<T> result = new ArrayList<>();
    for (T obj : inputs) {
      if (predicate.test(obj)) {
        result.add(obj);
      }
    }
    return result;
  }

  /**
   * Using the iterable collection and the predicate function passed in, removes from the
   * given collection, those items that do not satisfy the predicate.
   *
   * @param inputs - iterable collection to be modified
   * @param predicate - predicate function supplying the filter test
   */
  public static <T> void filterInPlace(Iterable<T> inputs, Predicate<T> predicate) {
    Iterator<T> iterator = inputs.iterator();
    while(iterator.hasNext()) {
      if(!predicate.test(iterator.next())) {
        iterator.remove();
      }
    }
  }

  /**
   * Performs a reduce operation on the passed collection using the passed reducer
   *
   * @param inputs an iterable of input elements
   * @param reducer reducer function
   * @param initialValue initial value passed to the reducer's reduce method along with the first element
   * @return reduction of the collection
   */
  public static <S,T> T reduce(Iterable<S> inputs, Reducer<S,T> reducer, T initialValue) {
    for (S next : inputs) {
      initialValue = reducer.reduce(initialValue, next);
    }
    return initialValue;
  }

  /**
   * Performs a reduce operation on the passed collection using the passed reducer function- a trinary
   * function that takes the (0-based) iteration index as a third argument.
   *
   * @param inputs an iterable of input elements
   * @param reducer reducer function (with index capture)
   * @param initialValue initial value passed to the reducer's reduce method along with the first element
   * @return reduction of the collection
   */
  public static <S,T> T reduceWithIndex(Iterable<S> inputs, TriFunction<T,S,Integer,T> reducer, T initialValue) {
    int index = 0;
    for (S next : inputs) {
      initialValue = reducer.apply(initialValue, next, index++);
    }
    return initialValue;
  }

  /**
   * Transforms the values of a map to new values linked to the same keys.  A new LinkedHashMap containing
   * the new values is returned (i.e. iteration order is maintained).  The old map is unmodified.  The new
   * map will be the same size as the old one.
   *
   * @param map a map from key to old value
   * @param transform a function to transform old values to new values
   * @return a new map containing the same keys pointing to new values
   */
  public static <R,S,T> Map<R,T> transformValues(Map<R,S> map, Function<S,T> transform) {
    Map<R,T> newMap = new LinkedHashMap<>(); // maintain iteration order of the incoming map
    for (Entry<R,S> entry : map.entrySet()) {
      newMap.put(entry.getKey(), transform.apply(entry.getValue()));
    }
    return newMap;
  }

  /**
   * Takes a function that may or may not have checked exceptions and returns a new function that performs
   * the same operation but "swallows" any checked exception by wrapping it in a RuntimeException and
   * throwing that instead.  If calling code wishes to inspect the underlying exception it must catch the
   * RuntimeException and use getCause().
   *
   * @param f function to wrap
   * @return a new function that swallows checked exceptions
   */
  public static <S,T> Function<S,T> fSwallow(FunctionWithException<S,T> f) {
    return x -> {
      try {
        return f.apply(x);
      }
      catch (Exception e) {
        throw (e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e));
      }
    };
  }

  /**
   * Takes a predicate that may or may not have checked exceptions and returns a new predicate that performs
   * the same operation but "swallows" any checked exception by wrapping it in a RuntimeException and
   * throwing that instead.  If calling code wishes to inspect the underlying exception it must catch the
   * RuntimeException and use getCause().
   *
   * @param f predicate to wrap
   * @return a new predicate that swallows checked exceptions
   */
  public static <T> Predicate<T> pSwallow(PredicateWithException<T> f) {
    return x -> {
      try {
        return f.test(x);
      }
      catch (Exception e) {
        throw (e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e));
      }
    };
  }

  /**
   * Takes a reducer that may or may not have checked exceptions and returns a new reducer that performs
   * the same operation but "swallows" any checked exception by wrapping it in a RuntimeException and
   * throwing that instead.  If calling code wishes to inspect the underlying exception it must catch the
   * RuntimeException and use getCause().
   *
   * @param r reducer to wrap
   * @return a new reducer that swallows checked exceptions
   */
  public static <S,T> Reducer<S,T> rSwallow(ReducerWithException<S,T> r) {
    return (accumulator, next) -> {
      try {
        return r.reduce(accumulator, next);
      }
      catch (Exception e) {
        throw (e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e));
      }
    };
  }

  /**
   * Takes a no-arg function that may or may not have checked exceptions and returns a new no-arg function
   * that performs the same operation but "swallows" any checked exception by wrapping it in a
   * RuntimeException and throwing that instead.  If calling code wishes to inspect the underlying exception
   * it must catch the RuntimeException and use getCause().
   *
   * @param f function to wrap
   * @return a new function that swallows checked exceptions
   */
  public static <T> Supplier<T> f0Swallow(SupplierWithException<T> f) {
    return () -> {
      try {
        return f.get();
      }
      catch (Exception e) {
        throw (e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e));
      }
    };
  }

  /**
   * Takes a 2-arg function that may or may not have checked exceptions and returns a new 2-arg function
   * that performs the same operation but "swallows" any checked exception by wrapping it in a
   * RuntimeException and throwing that instead.  If calling code wishes to inspect the underlying exception
   * it must catch the RuntimeException and use getCause().
   *
   * @param f function to wrap
   * @return a new function that swallows checked exceptions
   */
  public static <R,S,T> BiFunction<R,S,T> f2Swallow(BiFunctionWithException<R,S,T> f) {
    return (obj1, obj2) -> {
      try {
        return f.apply(obj1, obj2);
      }
      catch (Exception e) {
        throw (e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e));
      }
    };
  }

  /**
   * Takes a no-arg function that may throw an exception, calls it, and returns the result
   *
   * @param producer a function that produces a value from no arguments
   * @return the value the function produces
   */
  public static <T> T swallowAndGet(SupplierWithException<T> producer) {
    try {
      return producer.get();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Zips two Iterables of objects into a single List of "combined" objects.  Combined objects are generated
   * by the passed zipper function.  If the two input Iterables contain unequal numbers of objects, the
   * stopOnExhaustion parameter is used to determine whether to apply the combiner only until both Iterables
   * continue to produce objects (i.e. true is passed), or to continue adding items to the resulting list
   * until both Iterables have been exhausted (i.e. false is passed).  If the latter, the zipper function
   * will be called with the objects remaining in the unexhausted Iterable, and null as the parameter
   * previously provided from the exhausted Iterable.
   *
   * @param first an iterable over the first parameterized type
   * @param second an iterable over the second parameterized type
   * @param zipper function to combine items of each passed iterator into a combined object
   * @param stopOnExhaustion whether to stop adding combined items when one of the iterators ends
   * @return an ArrayList of the third parameterized type
   */
  public static <R,S,T> List<T> zipToList(Iterable<R> first, Iterable<S> second,
      BiFunction<R,S,T> zipper, boolean stopOnExhaustion) {
    ListBuilder<T> zippedItems = new ListBuilder<>();
    Iterator<R> col1Iter = first.iterator();
    Iterator<S> col2Iter = second.iterator();
    while (col1Iter.hasNext() && col2Iter.hasNext()) {
      zippedItems.add(zipper.apply(col1Iter.next(), col2Iter.next()));
    }
    if (!stopOnExhaustion) {
      if (col1Iter.hasNext()) {
        reduce(toIterable(col1Iter), (accumulator, next) -> accumulator.add(zipper.apply(next, null)), zippedItems);
      }
      else if (col2Iter.hasNext()) {
        reduce(toIterable(col2Iter), (accumulator, next) -> accumulator.add(zipper.apply(null, next)), zippedItems);
      }
    }
    return zippedItems.toList();
  }

  /**
   * Finds the zero-based index of the first item in the passed iterable that matches the predicate.  Index
   * is defined as the iteration order so for unordered iterables (e.g. HashSet), this function is probably
   * not useful.
   *
   * @param items iterable of items
   * @param predicate test on the type of item in items
   * @return index of the first item that passes the predicate, or -1 if none do
   */
  public static <S, T extends S> int findFirstIndex(Iterable<T> items, Predicate<S> predicate) {
    int i = 0;
    for (T item : items) {
      if (predicate.test(item)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  /**
   * Returns true if any elements in the passed iterable pass the test in the passed predicate
   *
   * @param items iterable of items
   * @param predicate test on the type of item in items
   * @return true if items contains a value that passes the predicate, else false
   */
  public static <S, T extends S> boolean contains(Iterable<T> items, Predicate<S> predicate) {
    return findFirstIndex(items, predicate) != -1;
  }

  /**
   * Attempts to retrieve a value from the passed supplier.  If an exception occurs, a default value
   * is returned and the exception is buried.
   *
   * @param f supplier function to execute
   * @param defaultValue value to return if supplier fails
   */
  public static <T> T defaultOnException(SupplierWithException<T> f, T defaultValue) {
    try {
      return f.get();
    }
    catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Takes a supplier that may throw an exception, and a mapper from that exception to a desired exception;
   * calls the supplier, throwing a mapped exception is something goes wrong.
   *
   * @param f supplier
   * @param exceptionMapper exception mapper
   * @return value supplied by supplier if successful
   * @throws S mapped exception if supplier is not successful
   */
  public static <T, S extends Exception> T mapException(SupplierWithException<T> f, Function<Exception, S> exceptionMapper) throws S {
    try {
      return f.get();
    }
    catch (Exception e) {
      throw exceptionMapper.apply(e);
    }
  }

  /**
   * Calls the passed supplier and wraps any throw exception with a RuntimeException
   *
   * @param f supplier
   * @return value supplied by the supplier if successful
   * @throws RuntimeException if not successful
   */
  public static <T> T wrapException(SupplierWithException<T> f) {
    return mapException(f, RuntimeException::new);
  }

  /**
   * Checks the passed list's size to ensure n is a valid index; if so, returns the
   * value at that index, else returns null.
   *
   * @param list a list
   * @param n an index into the list
   * @return value at the passed index or null if index is invalid
   */
  public static <T> T getNthOrNull(List<T> list, int n) {
    return n >= 0 && list.size() > n ? list.get(n) : null;
  }

  /**
   * Negate a given predicate
   *
   * @return Negated predicate
   */
  public static <T> Predicate<T> not(final Predicate<T> pred) {
    return pred.negate();
  }

  /**
   * Tries to get the next value from the passed supplier.  If successful,
   * returns an optional containing the supplied value; if not and an exception
   * is thrown, returns an empty optional.
   *
   * @param supplier supplier with exception
   * @return optional of supplied value, or empty optional if exception thrown
   */
  public static <S> Optional<S> optionalOnException(SupplierWithException<S> supplier) {
    try {
      return Optional.of(supplier.get());
    }
    catch (Exception e) {
      return Optional.empty();
    }
  }
}
