/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.java.subdoc;


import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.ArrayAddUnique;
import com.couchbase.client.java.kv.ArrayAppend;
import com.couchbase.client.java.kv.ArrayInsert;
import com.couchbase.client.java.kv.ArrayPrepend;
import com.couchbase.client.java.kv.Increment;
import com.couchbase.client.java.kv.Insert;
import com.couchbase.client.java.kv.MutateInMacro;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInResult;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.kv.PersistTo;
import com.couchbase.client.java.kv.Remove;
import com.couchbase.client.java.kv.ReplicateTo;
import com.couchbase.client.java.kv.StoreSemantics;
import com.couchbase.client.java.kv.Upsert;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.DAYS;

/**
 * A bridge between SDK 2's MutateInBuilder and SDK 3's MutateInResult.
 * <p>
 * It has the same builder methods as SDK 2's MutateInBuilder, but the execute method returns
 * SDK 3's {@link MutationResult} instead of SDK 2's {@code DocumentFragment}.
 * <p>
 * It is intended to ease migration from SDK 2 to SDK 3, but is *not* a drop-in replacement.
 * <p>
 * Create a new instance by calling {@link #create(com.couchbase.client.java.Collection, String)}.
 *
 * @deprecated This class is neither supported nor maintained by Couchbase.
 * Use at your own risk. Please migrate to Couchbase SDK 3's
 * {@link com.couchbase.client.java.Collection#mutateIn(String, List)}
 * as soon as possible.
 */
public class MutateInBuilder {
  private static final int RELATIVE_EXPIRY_CUTOFF_SECONDS = Math.toIntExact(DAYS.toSeconds(30));

  private final com.couchbase.client.java.Collection collection;
  private final String documentId;

  private final long defaultTimeout;
  private final TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;
  private final MutateInOptions options = MutateInOptions.mutateInOptions();

  private final List<MutateInSpec> specs = new ArrayList<>();

  public static MutateInBuilder create(
      com.couchbase.client.java.Collection collection,
      String documentId
  ) {
    return new MutateInBuilder(collection, documentId);
  }

  private MutateInBuilder(com.couchbase.client.java.Collection collection, String documentId) {
    this.documentId = requireNonNull(documentId);
    this.collection = requireNonNull(collection);
    this.defaultTimeout = collection.environment().timeoutConfig().kvTimeout().toMillis();
  }

  /**
   * Returns the ID of the document targeted by this mutation.
   * <p>
   * This method is not part of the SDK 2 API. It is provided
   * as a convenience, since SDK 3's MutateInResult does not currently
   * have an accessor for the document ID.
   */
  public String getDocumentId() {
    return documentId;
  }

  /**
   * Configure the SDK 3 {@link MutateInOptions} for this builder
   * by passing a callback that customizes the options.
   * <p>
   * This method is not part of the SDK 2 API. It is provided
   * as a convenience, in case you want to customize the
   * default serializer or some other SDK3-specific option.
   */
  public MutateInBuilder configureSdk3Options(Consumer<MutateInOptions> configurator) {
    configurator.accept(options);
    return this;
  }

  public MutateInResult execute(PersistTo persistTo) {
    return execute(persistTo, defaultTimeout, defaultTimeUnit);
  }

  public MutateInResult execute(long timeout, TimeUnit timeUnit) {
    options.timeout(Duration.ofMillis(timeUnit.toMillis(timeout)));
    return execute();
  }

  public MutateInResult execute(PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
    withDurability(persistTo, replicateTo);
    options.timeout(Duration.ofMillis(timeUnit.toMillis(timeout)));
    return execute();
  }

  public MutateInResult execute(PersistTo persistTo, long timeout, TimeUnit timeUnit) {
    withDurability(persistTo);
    options.timeout(Duration.ofMillis(timeUnit.toMillis(timeout)));
    return execute();
  }

  public MutateInResult execute(ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
    withDurability(replicateTo);
    options.timeout(Duration.ofMillis(timeUnit.toMillis(timeout)));
    return execute();
  }

  public MutateInResult execute() {
    return collection.mutateIn(documentId, specs, options);
  }

  /**
   * Bridge to SDK 3's {@link MutateInOptions#expiry(Duration)} or {@link MutateInOptions#expiry(Instant)}
   *
   * @param expiry If <= the number of seconds in 30 days, interpreted as a duration in seconds.
   * Otherwise, interpreted as an absolute epoch second.
   */
  public MutateInBuilder withExpiry(int expiry) {
    if (expiry <= RELATIVE_EXPIRY_CUTOFF_SECONDS) {
      options.expiry(Duration.ofSeconds(expiry));
    } else {
      options.expiry(Instant.ofEpochSecond(expiry));
    }
    return this;
  }

  /**
   * Bridge to SDK 3's {@link MutateInOptions#cas(long)}.
   */
  public MutateInBuilder withCas(long cas) {
    options.cas(cas);
    return this;
  }

  /**
   * Bridge to SDK 3's {@link MutateInOptions#durability(PersistTo, ReplicateTo)}.
   */
  public MutateInBuilder withDurability(PersistTo persistTo) {
    return withDurability(persistTo, options.build().replicateTo());
  }

  /**
   * Bridge to SDK 3's {@link MutateInOptions#durability(PersistTo, ReplicateTo)}.
   */
  public MutateInBuilder withDurability(ReplicateTo replicateTo) {
    return withDurability(options.build().persistTo(), replicateTo);
  }

  /**
   * Bridge to SDK 3's {@link MutateInOptions#durability(PersistTo, ReplicateTo)}.
   */
  public MutateInBuilder withDurability(PersistTo persistTo, ReplicateTo replicateTo) {
    options.durability(
        Optional.of(persistTo).orElse(PersistTo.NONE),
        Optional.of(replicateTo).orElse(ReplicateTo.NONE)
    );
    return this;
  }

  /**
   * Bridge to SDK 3's {@link MutateInOptions#storeSemantics(StoreSemantics)} (which defaults to {@link StoreSemantics#REPLACE} if no semantics are specified).
   * <p>
   * Passing true means {@link StoreSemantics#UPSERT}, false means {@link StoreSemantics#REPLACE}.
   *
   * @deprecated This method does exactly the same thing as {@link #upsertDocument(boolean)}.
   */
  @Deprecated
  public MutateInBuilder createDocument(boolean createDocument) {
    return upsertDocument(createDocument);
  }

  /**
   * Bridge to SDK 3's {@link MutateInOptions#storeSemantics(StoreSemantics)} (which defaults to {@link StoreSemantics#REPLACE} if no semantics are specified).
   * <p>
   * Passing true means {@link StoreSemantics#UPSERT}, false means {@link StoreSemantics#REPLACE}.
   */
  public MutateInBuilder upsertDocument(boolean upsertDocument) {
    options.storeSemantics(upsertDocument ? StoreSemantics.UPSERT : StoreSemantics.REPLACE);
    return this;
  }

  /**
   * Bridge to SDK 3's {@link MutateInOptions#storeSemantics(StoreSemantics)} (which defaults to {@link StoreSemantics#REPLACE} if no semantics are specified).
   * <p>
   * Passing true means {@link StoreSemantics#INSERT}, false means {@link StoreSemantics#REPLACE}.
   */
  public MutateInBuilder insertDocument(boolean insertDocument) {
    options.storeSemantics(insertDocument ? StoreSemantics.INSERT : StoreSemantics.REPLACE);
    return this;
  }

  public <T> MutateInBuilder replace(String path, T fragment) {
    return add(MutateInSpec.replace(path, fragment));
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#insert(String, Object)}.
   */
  @Deprecated
  public <T> MutateInBuilder insert(String path, T fragment, boolean createPath) {
    return insert(path, fragment, new SubdocOptionsBuilder().createPath(createPath));
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#insert(String, Object)}.
   */
  public <T> MutateInBuilder insert(String path, T fragment) {
    return insert(path, fragment, new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#insert(String, Object)}.
   */
  public <T> MutateInBuilder insert(String path, T fragment, SubdocOptionsBuilder optionsBuilder) {
    Insert op = MutateInSpec.insert(path, translateMacros(fragment, optionsBuilder));
    if (optionsBuilder.createPath()) {
      op.createPath();
    }
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#upsert(String, Object)}.
   */
  @Deprecated
  public <T> MutateInBuilder upsert(String path, T fragment, boolean createPath) {
    return upsert(path, fragment, new SubdocOptionsBuilder().createPath(createPath));
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#upsert(String, Object)}.
   */
  public <T> MutateInBuilder upsert(String path, T fragment) {
    return upsert(path, fragment, new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#upsert(String, Object)}.
   */
  public MutateInBuilder upsert(JsonObject content) {
    return upsert("", content);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#upsert(String, Object)}.
   */
  public <T> MutateInBuilder upsert(String path, T fragment, SubdocOptionsBuilder optionsBuilder) {
    Upsert op = MutateInSpec.upsert(path, translateMacros(fragment, optionsBuilder));
    if (optionsBuilder.createPath()) {
      op.createPath();
    }
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#remove(String)}.
   */
  public <T> MutateInBuilder remove(String path) {
    return remove(path, new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#remove(String)}.
   */
  public <T> MutateInBuilder remove(String path, SubdocOptionsBuilder optionsBuilder) {
    Remove op = MutateInSpec.remove(path);
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#increment(String, long)}.
   * <p>
   * NOTE: SDK 3 also has a {@link MutateInSpec#decrement(String, long)} method, but it's the same as passing a negative number to {@code increment}.
   */
  @Deprecated
  public MutateInBuilder counter(String path, long delta, boolean createPath) {
    return counter(path, delta, new SubdocOptionsBuilder().createPath(createPath));
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#increment(String, long)}.
   * <p>
   * NOTE: SDK 3 also has a {@link MutateInSpec#decrement(String, long)} method, but it's the same as passing a negative number to {@code increment}.
   */
  public MutateInBuilder counter(String path, long delta) {
    return counter(path, delta, new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#increment(String, long)}.
   * <p>
   * NOTE: SDK 3 also has a {@link MutateInSpec#decrement(String, long)} method, but it's the same as passing a negative number to {@code increment}.
   */
  public MutateInBuilder counter(String path, long delta, SubdocOptionsBuilder optionsBuilder) {
    Increment op = MutateInSpec.increment(path, delta);
    if (optionsBuilder.createPath()) {
      op.createPath();
    }
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayPrepend(String, List)}.
   */
  @Deprecated
  public <T> MutateInBuilder arrayPrepend(String path, T value, boolean createPath) {
    return arrayPrepend(path, value, new SubdocOptionsBuilder().createPath(createPath));
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayPrepend(String, List)}.
   */
  public <T> MutateInBuilder arrayPrepend(String path, T value) {
    return arrayPrepend(path, value, new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayPrepend(String, List)}.
   */
  public <T> MutateInBuilder arrayPrepend(String path, T value, SubdocOptionsBuilder optionsBuilder) {
    return arrayPrependAll(path, singletonList(value), optionsBuilder);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayPrepend(String, List)}.
   */
  @Deprecated
  public <T> MutateInBuilder arrayPrependAll(String path, Collection<T> values, boolean createPath) {
    return arrayPrependAll(path, values, new SubdocOptionsBuilder().createPath(createPath));
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayPrepend(String, List)}.
   */
  public <T> MutateInBuilder arrayPrependAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
    ArrayPrepend op = MutateInSpec.arrayPrepend(path, new ArrayList<>(values));
    if (optionsBuilder.createPath()) {
      op.createPath();
    }
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayPrepend(String, List)}.
   */
  public <T> MutateInBuilder arrayPrependAll(String path, T... values) {
    return arrayPrependAll(path, Arrays.asList(values), new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAppend(String, List)}.
   */
  @Deprecated
  public <T> MutateInBuilder arrayAppend(String path, T value, boolean createPath) {
    return arrayAppendAll(path, singletonList(value), new SubdocOptionsBuilder().createPath(createPath));
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAppend(String, List)}.
   */
  public <T> MutateInBuilder arrayAppend(String path, T value) {
    return arrayAppendAll(path, singletonList(value), new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAppend(String, List)}.
   */
  public <T> MutateInBuilder arrayAppend(String path, T value, SubdocOptionsBuilder optionsBuilder) {
    return arrayAppendAll(path, singletonList(value), optionsBuilder);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAppend(String, List)}.
   */
  @Deprecated
  public <T> MutateInBuilder arrayAppendAll(String path, Collection<T> values, boolean createPath) {
    return arrayAppendAll(path, values, SubdocOptionsBuilder.builder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAppend(String, List)}.
   */
  public <T> MutateInBuilder arrayAppendAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
    ArrayAppend op = MutateInSpec.arrayAppend(path, new ArrayList<>(values));
    if (optionsBuilder.createPath()) {
      op.createPath();
    }
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAppend(String, List)}.
   */
  public <T> MutateInBuilder arrayAppendAll(String path, T... values) {
    return arrayAppendAll(path, Arrays.asList(values), new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayInsert(String, List)}.
   */
  public <T> MutateInBuilder arrayInsert(String path, T value) {
    return arrayInsert(path, value, new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayInsert(String, List)}.
   */
  public <T> MutateInBuilder arrayInsert(String path, T value, SubdocOptionsBuilder optionsBuilder) {
    return arrayInsertAll(path, singletonList(value), optionsBuilder);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayInsert(String, List)}.
   */
  public <T> MutateInBuilder arrayInsertAll(String path, Collection<T> values) {
    return arrayInsertAll(path, values, new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayInsert(String, List)}.
   */
  public <T> MutateInBuilder arrayInsertAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
    ArrayInsert op = MutateInSpec.arrayInsert(path, new ArrayList<>(values));
    if (optionsBuilder.createPath()) {
      op.createPath();
    }
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayInsert(String, List)}.
   */
  public <T> MutateInBuilder arrayInsertAll(String path, T... values) {
    return arrayInsertAll(path, Arrays.asList(values), new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAddUnique(String, Object)}.
   */
  @Deprecated
  public <T> MutateInBuilder arrayAddUnique(String path, T value, boolean createPath) {
    return arrayAddUnique(path, value, new SubdocOptionsBuilder().createPath(createPath));
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAddUnique(String, Object)}.
   */
  public <T> MutateInBuilder arrayAddUnique(String path, T value) {
    return arrayAddUnique(path, value, new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link MutateInSpec#arrayAddUnique(String, Object)}.
   */
  public <T> MutateInBuilder arrayAddUnique(String path, T value, SubdocOptionsBuilder optionsBuilder) {
    ArrayAddUnique op = MutateInSpec.arrayAddUnique(path, translateMacros(value, optionsBuilder));
    if (optionsBuilder.createPath()) {
      op.createPath();
    }
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return add(op);
  }

  // Visible for testing
  MutateInOptions.Built builtOptions() {
    return options.build();
  }

  private MutateInBuilder add(MutateInSpec spec) {
    specs.add(spec);
    return this;
  }

  private static Object translateMacros(Object in, SubdocOptionsBuilder subdocOptionsBuilder) {
    if (!subdocOptionsBuilder.expandMacros() || !(in instanceof String)) {
      return in;
    }
    switch ((String) in) {
      case "${Mutation.CAS}":
        return MutateInMacro.CAS;
      case "${Mutation.seqno}":
        return MutateInMacro.SEQ_NO;
      case "${Mutation.value_crc32c}":
        return MutateInMacro.VALUE_CRC_32C;
      default:
        return in;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("mutateIn(").append(documentId);
    MutateInOptions.Built opts = options.build();
    long expiry = opts.expiry().encode();
    if (expiry != 0) {
      sb.append(", expiry=").append(expiry);
    }

    if (opts.cas() != 0L) {
      sb.append(", cas=").append(opts.cas());
    }

    if (opts.persistTo() != PersistTo.NONE) {
      sb.append(", persistTo=").append(opts.persistTo());
    }

    if (opts.replicateTo() != ReplicateTo.NONE) {
      sb.append(", replicateTo=").append(opts.replicateTo());
    }

    sb.append(")");
    sb.append(specs);
    return sb.toString();
  }
}
