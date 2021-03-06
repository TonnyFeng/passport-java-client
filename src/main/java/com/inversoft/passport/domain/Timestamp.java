/*
 * Copyright (c) 2015, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.inversoft.passport.domain;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.inversoft.json.ToString;

/**
 * Timestamp.
 *
 * @author Brian Pontarelli
 */
public class Timestamp {
  public ZonedDateTime instant;

  public String name;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Timestamp)) {
      return false;
    }
    Timestamp timestamp = (Timestamp) o;
    return Objects.equals(instant, timestamp.instant) &&
        Objects.equals(name, timestamp.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instant, name);
  }

  @Override
  public String toString() {
    return ToString.toString(this);
  }
}
