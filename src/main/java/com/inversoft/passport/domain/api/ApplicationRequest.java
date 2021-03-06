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
package com.inversoft.passport.domain.api;

import java.util.List;
import java.util.UUID;

import com.inversoft.json.JacksonConstructor;
import com.inversoft.passport.domain.Application;
import com.inversoft.passport.domain.ApplicationRole;

/**
 * The Application API request object.
 *
 * @author Brian Pontarelli
 */
public class ApplicationRequest {
  public Application application;

  public List<UUID> notificationServerIds;

  public ApplicationRole role;

  @JacksonConstructor
  public ApplicationRequest() {
  }

  public ApplicationRequest(Application application, List<UUID> notificationServerIds) {
    this.application = application;
    this.notificationServerIds = notificationServerIds;
  }

  public ApplicationRequest(ApplicationRole role) {
    this.role = role;
  }
}
