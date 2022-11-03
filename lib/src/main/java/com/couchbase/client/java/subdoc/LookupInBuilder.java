/*
 * Copyright 2022 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.java.subdoc;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.LookupInOptions;
import com.couchbase.client.java.kv.LookupInResult;
import com.couchbase.client.java.kv.LookupInSpec;
import com.couchbase.client.java.kv.LookupInSpecStandard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A bridge between SDK 2's LookupInBuilder and SDK 3's LookupInResult.
 * <p>
 * It has the same builder methods as SDK 2's LookInBuilder, but the execute method returns
 * SDK 3's {@link LookupInResult} instead of SDK 2's {@code DocumentFragment}.
 * <p>
 * It is intended to ease migration from SDK 2 to SDK 3, but is *not* a drop-in replacement.
 * <p>
 * Create a new instance by calling {@link #create(Collection, String)}.
 *
 * @deprecated This class is neither supported nor maintained by Couchbase.
 * Use at your own risk. Please migrate to Couchbase SDK 3's
 * {@link com.couchbase.client.java.Collection#lookupIn(String, List)}
 * as soon as possible.
 */
@Deprecated
public class LookupInBuilder {
  private final com.couchbase.client.java.Collection collection;
  private final String documentId;

  private final long defaultTimeout;
  private final TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;
  private final LookupInOptions options = LookupInOptions.lookupInOptions();
  private final List<LookupInSpec> specs = new ArrayList<>();

  public static LookupInBuilder create(
      com.couchbase.client.java.Collection collection,
      String documentId
  ) {
    return new LookupInBuilder(collection, documentId);
  }

  private LookupInBuilder(com.couchbase.client.java.Collection collection, String documentId) {
    this.documentId = requireNonNull(documentId);
    this.collection = requireNonNull(collection);
    this.defaultTimeout = collection.environment().timeoutConfig().kvTimeout().toMillis();
  }

  /**
   * Returns the ID of the document targeted by this lookup.
   * <p>
   * This method is not part of the SDK 2 API. It is provided
   * as a convenience, since SDK 3'sLookupInResult does not currently
   * have an accessor for the document ID.
   */
  public String getDocumentId() {
    return documentId;
  }

  /**
   * Configure the SDK 3 {@link LookupInOptions} for this builder
   * by passing a callback that customizes the options.
   * <p>
   * This method is not part of the SDK 2 API. It is provided
   * as a convenience, in case you want to customize the
   * default serializer or some other SDK3-specific option.
   */
  public LookupInBuilder configureSdk3Options(Consumer<LookupInOptions> configurator) {
    configurator.accept(options);
    return this;
  }

  /**
   * Bridge for SDK 3's {@link LookupInOptions#accessDeleted(boolean)}.
   */
  public LookupInBuilder accessDeleted(boolean accessDeleted) {
    options.accessDeleted(accessDeleted);
    return this;
  }

  public LookupInResult execute() {
    return execute(defaultTimeout, defaultTimeUnit);
  }

  public LookupInResult execute(long timeout, TimeUnit timeUnit) {
    options.timeout(Duration.ofMillis(timeUnit.toMillis(timeout)));
    return collection.lookupIn(documentId, specs, options);
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#get(String)}.
   */
  public LookupInBuilder get(String... paths) {
    return get(Arrays.asList(paths), new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#get(String)}.
   */
  public LookupInBuilder get() {
    return get("");
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#get(String)}.
   */
  public LookupInBuilder get(String path, SubdocOptionsBuilder optionsBuilder) {
    LookupInSpecStandard op = applyOptions(LookupInSpec.get(path), optionsBuilder);
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#get(String)}.
   */
  public LookupInBuilder get(Iterable<String> paths, SubdocOptionsBuilder optionsBuilder) {
    paths.forEach(p -> get(p, optionsBuilder));
    return this;
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#exists(String)}.
   */
  public LookupInBuilder exists(String... paths) {
    return exists(Arrays.asList(paths), new SubdocOptionsBuilder());
  }

  public LookupInBuilder exists(String path, SubdocOptionsBuilder optionsBuilder) {
    LookupInSpec op = applyOptions(LookupInSpec.exists(path), optionsBuilder);
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#exists(String)}.
   */
  public LookupInBuilder exists(Iterable<String> paths, SubdocOptionsBuilder optionsBuilder) {
    paths.forEach(p -> exists(p, optionsBuilder));
    return this;
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#count(String)}.
   */
  public LookupInBuilder getCount(String... paths) {
    return getCount(Arrays.asList(paths), new SubdocOptionsBuilder());
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#count(String)}.
   */
  public LookupInBuilder getCount(String path, SubdocOptionsBuilder optionsBuilder) {
    LookupInSpec op = applyOptions(LookupInSpec.count(path), optionsBuilder);
    return add(op);
  }

  /**
   * Bridge to SDK 3's {@link LookupInSpec#count(String)}.
   */
  public LookupInBuilder getCount(Iterable<String> paths, SubdocOptionsBuilder optionsBuilder) {
    paths.forEach(p -> getCount(p, optionsBuilder));
    return this;
  }

  /**
   * Does nothing. "Raw" results are always available in SDK 3 by calling
   * {@link LookupInResult#contentAsBytes(int)}.
   */
  public LookupInBuilder includeRaw(boolean includeRaw) {
    // noop, can always get raw with SDK 3
    return this;
  }


  /**
   * Always returns true. "Raw" results are always available in SDK 3 by calling
   * {@link LookupInResult#contentAsBytes(int)}.
   */
  public boolean isIncludeRaw() {
    return true;
  }

  private static LookupInSpecStandard applyOptions(LookupInSpecStandard op, SubdocOptionsBuilder optionsBuilder) {
    if (optionsBuilder.xattr()) {
      op.xattr();
    }
    return op;
  }

  private LookupInBuilder add(LookupInSpec spec) {
    specs.add(spec);
    return this;
  }


  @Override
  public String toString() {
    return "lookupIn(" + documentId + ")" + specs;
  }
}
