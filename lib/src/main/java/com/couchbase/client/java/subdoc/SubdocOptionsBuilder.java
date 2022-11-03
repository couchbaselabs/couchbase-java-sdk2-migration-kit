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

import java.util.List;

/**
 * A bridge between SDK 2's SubdocOptionsBuilder and SDK 3's lookup / mutation spec modifiers.
 * <p>
 * It is intended to ease migration from SDK 2 to SDK 3, but is *not* a drop-in replacement.
 *
 * @deprecated This class is neither supported nor maintained by Couchbase.
 * Use at your own risk. Please migrate to Couchbase SDK 3's
 * {@link Collection#mutateIn(String, List)} and
 * {@link Collection#lookupIn(String, List)} as soon as possible.
 */
@Deprecated
public class SubdocOptionsBuilder {
  private boolean createPath;
  private boolean xattr;
  private boolean expandMacros;

  public SubdocOptionsBuilder() {
  }

  public static SubdocOptionsBuilder builder() {
    return new SubdocOptionsBuilder();
  }

  /**
   * @deprecated Please use {@link #createPath(boolean)} instead. It does the same thing.
   */
  @Deprecated
  public SubdocOptionsBuilder createParents(boolean createParents) {
    return createPath(createParents);
  }

  /**
   * In SDK 3, this setting is a modifier on the MutateInSpec. For example:
   * <pre>
   * Upsert spec = MutateInSpec.upsert("foo", "bar").createPath();
   * </pre>
   */
  public SubdocOptionsBuilder createPath(boolean createPath) {
    this.createPath = createPath;
    return this;
  }

  /**
   * Returns the current value of the "create path" option.
   */
  @Deprecated
  public boolean createParents() {
    return createPath();
  }

  /**
   * Returns the current value of the "create path" option.
   */
  public boolean createPath() {
    return createPath;
  }

  /**
   * In SDK 3, this setting is a modifier on the MutateInSpec. For example:
   * <pre>
   * Upsert spec = MutateInSpec.upsert("foo", "bar").xattr();
   * </pre>
   */
  public SubdocOptionsBuilder xattr(boolean xattr) {
    this.xattr = xattr;
    return this;
  }

  /**
   * Returns the current value of the "xattr" option.
   */
  public boolean xattr() {
    return this.xattr;
  }

  /**
   * Controls whether macros such as ${Mutation.CAS} will be expanded by the server for this field.
   * Default is false.
   * <p>
   * This option does not exist in SDK 3. Instead, pass one of the {@link com.couchbase.client.java.kv.MutateInMacro}
   * enum values instead of the macro string. For example:
   * <pre>
   * Upsert spec = MutateInSpec.upsert("foo", MutateInMacro.SEQ_NO).xattr();
   * </pre>
   */
  public SubdocOptionsBuilder expandMacros(boolean expandMacros) {
    this.expandMacros = expandMacros;
    return this;
  }

  /**
   * Returns the current value of the "expandMacros" option.
   */
  public boolean expandMacros() {
    return this.expandMacros;
  }

  @Override
  public String toString() {
    return "{" +
        " \"createPath\": " + createPath +
        ", \"xattr\":" + xattr +
        ", \"expandMacros\":" + expandMacros +
        "}";
  }
}
