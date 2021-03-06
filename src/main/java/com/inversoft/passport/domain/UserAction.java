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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inversoft.json.ToString;
import com.inversoft.passport.domain.util.Normalizer;

/**
 * An action that can be executed on a user (discipline or reward potentially).
 *
 * @author Brian Pontarelli
 */
public class UserAction implements Comparable<UserAction> {
  public boolean active;

  /**
   * Only time-based actions. Template to use when cancelled
   */
  public UUID cancelEmailTemplateId;

  /**
   * Only time-based actions. Template to use when ended
   */
  public UUID endEmailTemplateId;

  public UUID id;

  public boolean includeEmailInNotificationJson;

  public LocalizedStrings localizedNames;

  /**
   * Only time-based actions. Template to use when modified
   */
  public UUID modifyEmailTemplateId;

  public String name;

  public List<UserActionOption> options = new ArrayList<>();

  public boolean preventLogin;

  /**
   * Only time-based actions. This indicates passport will send a notification when the action expires
   */
  public boolean sendEndNotification;

  /**
   * All actions. The template to be used when an action is first taken
   */
  public UUID startEmailTemplateId;

  public boolean temporal;

  /**
   * Passport emailing
   */
  public boolean userEmailingEnabled;

  /**
   * flag in notification instructing notification servers to notify user
   */
  public boolean userNotificationsEnabled;

  public UserAction() {
  }

  public UserAction(String name) {
    this.name = name;
  }

  public UserAction(UUID id, String name, boolean active, LocalizedStrings localizedNames, boolean preventLogin,
                    boolean sendEndNotification, boolean temporal, boolean userNotificationsEnabled,
                    boolean userEmailingEnabled, boolean includeEmailInNotificationJson,
                    UUID startEmailTemplateId, UUID modifyEmailTemplateId, UUID cancelEmailTemplateId,
                    UUID endEmailTemplateId, UserActionOption... options) {
    this.id = id;
    this.active = active;
    this.name = name;
    this.includeEmailInNotificationJson = includeEmailInNotificationJson;
    this.localizedNames = localizedNames;
    this.preventLogin = preventLogin;
    this.sendEndNotification = sendEndNotification;
    this.temporal = temporal;
    this.userNotificationsEnabled = userNotificationsEnabled;
    this.startEmailTemplateId = startEmailTemplateId;
    this.modifyEmailTemplateId = modifyEmailTemplateId;
    this.cancelEmailTemplateId = cancelEmailTemplateId;
    this.endEmailTemplateId = endEmailTemplateId;
    this.userEmailingEnabled = userEmailingEnabled;
    Collections.addAll(this.options, options);
  }

  @Override
  public int compareTo(UserAction o) {
    return name.compareTo(o.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserAction)) {
      return false;
    }
    UserAction that = (UserAction) o;
    return Objects.equals(active, that.active) &&
        Objects.equals(includeEmailInNotificationJson, that.includeEmailInNotificationJson) &&
        Objects.equals(preventLogin, that.preventLogin) &&
        Objects.equals(sendEndNotification, that.sendEndNotification) &&
        Objects.equals(temporal, that.temporal) &&
        Objects.equals(userNotificationsEnabled, that.userNotificationsEnabled) &&
        Objects.equals(userEmailingEnabled, that.userEmailingEnabled) &&
        Objects.equals(localizedNames, that.localizedNames) &&
        Objects.equals(name, that.name) &&
        Objects.equals(options, that.options) &&
        Objects.equals(startEmailTemplateId, that.startEmailTemplateId) &&
        Objects.equals(modifyEmailTemplateId, that.modifyEmailTemplateId) &&
        Objects.equals(cancelEmailTemplateId, that.cancelEmailTemplateId) &&
        Objects.equals(endEmailTemplateId, that.endEmailTemplateId);
  }

  @JsonIgnore
  public UserActionOption getOption(String name) {
    if (name == null) {
      return null;
    }

    for (UserActionOption key : options) {
      if (key.name.equals(name)) {
        return key;
      }
    }

    return null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(active, includeEmailInNotificationJson, localizedNames, name, options, preventLogin, sendEndNotification,
        temporal, userNotificationsEnabled, userEmailingEnabled, startEmailTemplateId, modifyEmailTemplateId, cancelEmailTemplateId,
        endEmailTemplateId);
  }

  public void normalize() {
    name = Normalizer.trim(name);

    if (localizedNames != null) {
      localizedNames.normalize();
    }

    if (options != null) {
      options.forEach(UserActionOption::normalize);
    }
  }

  public void sortOptions() {
    Collections.sort(options);
  }

  public String toString() {
    return ToString.toString(this);
  }

  public boolean usesEmailTemplate(UUID emailTemplateId) {
    return (startEmailTemplateId != null && startEmailTemplateId.equals(emailTemplateId)) ||
        (modifyEmailTemplateId != null && modifyEmailTemplateId.equals(emailTemplateId)) ||
        (cancelEmailTemplateId != null && cancelEmailTemplateId.equals(emailTemplateId)) ||
        (endEmailTemplateId != null && endEmailTemplateId.equals(emailTemplateId));
  }
}
