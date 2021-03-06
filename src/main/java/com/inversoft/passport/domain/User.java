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

import java.net.URI;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inversoft.json.ToString;
import static com.inversoft.passport.domain.util.Normalizer.toLowerCase;
import static com.inversoft.passport.domain.util.Normalizer.trim;

/**
 * The global view of a User. This object contains all global information about the user including birth date,
 * registration information preferred languages, global attributes, etc.
 *
 * @author Seth Musselman
 */
public class User implements Buildable<User> {
  private final List<UUID> childIds = new ArrayList<>();

  private final List<UserRegistration> registrations = new ArrayList<>();

  public boolean active;

  public LocalDate birthDate;

  public UUID cleanSpeakId;

  public ParentalConsentType parentalConsentType;

  public UserData data;

  public String email;

  public String encryptionScheme;

  public ZonedDateTime expiry;

  public String firstName;

  public String fullName;

  public UUID id;

  public URI imageUrl;

  public ZonedDateTime lastLoginInstant;

  public String lastName;

  public String middleName;

  public String mobilePhone;

  public UUID parentId;

  public String password;

  public boolean passwordChangeRequired;

  public String salt;

  public String timezone;

  public boolean twoFactorEnabled;

  public String twoFactorSecret;

  public String username;

  public ContentStatus usernameStatus;

  public String verificationId;

  public ZonedDateTime verificationIdCreateInstant;

  public boolean verified;

  public User() {
  }

  public User(UUID id, String email, String username, String password, String salt, LocalDate birthDate,
              String fullName, String firstName, String middleName, String lastName, String encryptionScheme,
              ZonedDateTime expiry, boolean active, String timezone, UUID cleanSpeakId, UserData data, boolean verified,
              String verificationId, ContentStatus usernameStatus, String twoFactorSecret,
              URI imageUri, UserRegistration... registrations) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.salt = salt;
    this.birthDate = birthDate;
    this.encryptionScheme = encryptionScheme;
    this.expiry = expiry;
    this.active = active;
    this.username = username;
    this.timezone = timezone;
    this.fullName = fullName;
    this.firstName = firstName;
    this.middleName = middleName;
    this.lastName = lastName;
    this.data = data;
    this.cleanSpeakId = cleanSpeakId;
    this.verificationId = verificationId;
    this.verified = verified;
    this.usernameStatus = usernameStatus;
    this.twoFactorSecret = twoFactorSecret;
    this.imageUrl = imageUri;
    Collections.addAll(this.registrations, registrations);
    normalize();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof User)) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(active, user.active) &&
        Objects.equals(birthDate, user.birthDate) &&
        Objects.equals(childIds, user.childIds) &&
        Objects.equals(cleanSpeakId, user.cleanSpeakId) &&
        Objects.equals(parentalConsentType, user.parentalConsentType) &&
        Objects.equals(data, user.data) &&
        Objects.equals(encryptionScheme, user.encryptionScheme) &&
        Objects.equals(email, user.email) &&
        Objects.equals(expiry, user.expiry) &&
        Objects.equals(firstName, user.firstName) &&
        Objects.equals(fullName, user.fullName) &&
        Objects.equals(imageUrl, user.imageUrl) &&
        Objects.equals(lastLoginInstant, user.lastLoginInstant) &&
        Objects.equals(lastName, user.lastName) &&
        Objects.equals(middleName, user.middleName) &&
        Objects.equals(mobilePhone, user.mobilePhone) &&
        Objects.equals(parentId, user.parentId) &&
        Objects.equals(password, user.password) &&
        Objects.equals(passwordChangeRequired, user.passwordChangeRequired) &&
        Objects.equals(registrations, user.registrations) &&
        Objects.equals(salt, user.salt) &&
        Objects.equals(timezone, user.timezone) &&
        Objects.equals(twoFactorSecret, user.twoFactorSecret) &&
        Objects.equals(username, user.username) &&
        Objects.equals(usernameStatus, user.usernameStatus) &&
        Objects.equals(verificationId, user.verificationId) &&
        Objects.equals(verificationIdCreateInstant, user.verificationIdCreateInstant) &&
        Objects.equals(verified, user.verified);
  }

  public List<UUID> getChildIds() {
    return childIds;
  }

  public UserData getDataForApplication(UUID id) {
    Optional<UserRegistration> registration = getRegistrations().stream()
                                                                .filter((reg) -> reg.applicationId.equals(id))
                                                                .findFirst();
    if (registration.isPresent()) {
      return registration.get().data;
    }

    return null;
  }

  /**
   * Used in UI for mobile
   */
  @JsonIgnore
  public String getLogin() {
    return email == null ? username : email;
  }

  @JsonIgnore
  public String getName() {
    if (fullName != null) {
      return fullName;
    }
    if (firstName != null) {
      return firstName + (lastName != null ? " " + lastName : "");
    }

    return null;
  }

  @JsonIgnore
  public List<Locale> getPreferredLanguages() {
    return data == null ? Collections.emptyList() : data.preferredLanguages;
  }

  public UserRegistration getRegistrationForApplication(UUID id) {
    Optional<UserRegistration> registration = getRegistrations().stream()
                                                                .filter((reg) -> reg.applicationId.equals(id))
                                                                .findFirst();
    if (registration.isPresent()) {
      return registration.get();
    }

    return null;
  }

  public List<UserRegistration> getRegistrations() {
    return registrations;
  }

  public Set<String> getRoleNamesForApplication(UUID id) {
    Optional<UserRegistration> registration = getRegistrations().stream()
                                                                .filter((reg) -> reg.applicationId.equals(id))
                                                                .findFirst();
    if (registration.isPresent()) {
      return registration.get().roles;
    }

    return null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(active, birthDate, childIds, cleanSpeakId, parentalConsentType, data, email, encryptionScheme, expiry,
        firstName, fullName, imageUrl, lastLoginInstant, lastName, middleName, mobilePhone, parentId, password, passwordChangeRequired,
        registrations, salt, timezone, twoFactorSecret, username, usernameStatus, verificationId,
        verificationIdCreateInstant, verified);
  }

  /**
   * Normalizes all of the fields.
   */
  public void normalize() {
    email = toLowerCase(trim(email));
    if (data != null) {
      data.normalize();
    }
    encryptionScheme = trim(encryptionScheme);
    firstName = trim(firstName);
    fullName = trim(fullName);
    lastName = trim(lastName);
    middleName = trim(middleName);
    mobilePhone = trim(mobilePhone);
    timezone = trim(timezone);
    username = trim(username);
    getRegistrations().forEach(UserRegistration::normalize);
  }

  /**
   * Clear out the password and salt fields
   */
  public User secure() {
    salt = null;
    password = null;
    twoFactorEnabled = twoFactorSecret != null;
    twoFactorSecret = null;
    return this;
  }

  @Override
  public String toString() {
    return ToString.toString(this);
  }
}
