/*
 * Copyright (c) 2015-2016, Inversoft Inc., All Rights Reserved
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
package com.inversoft.passport.client;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.inversoft.error.Errors;
import com.inversoft.passport.domain.AuditLog;
import com.inversoft.passport.domain.api.ApplicationRequest;
import com.inversoft.passport.domain.api.ApplicationResponse;
import com.inversoft.passport.domain.api.AuditLogRequest;
import com.inversoft.passport.domain.api.AuditLogResponse;
import com.inversoft.passport.domain.api.EmailTemplateRequest;
import com.inversoft.passport.domain.api.EmailTemplateResponse;
import com.inversoft.passport.domain.api.LoginRequest;
import com.inversoft.passport.domain.api.LoginResponse;
import com.inversoft.passport.domain.api.NotificationServerRequest;
import com.inversoft.passport.domain.api.NotificationServerResponse;
import com.inversoft.passport.domain.api.PreviewRequest;
import com.inversoft.passport.domain.api.PreviewResponse;
import com.inversoft.passport.domain.api.SystemConfigurationRequest;
import com.inversoft.passport.domain.api.SystemConfigurationResponse;
import com.inversoft.passport.domain.api.TwoFactorRequest;
import com.inversoft.passport.domain.api.UserActionReasonRequest;
import com.inversoft.passport.domain.api.UserActionReasonResponse;
import com.inversoft.passport.domain.api.UserActionRequest;
import com.inversoft.passport.domain.api.UserActionResponse;
import com.inversoft.passport.domain.api.UserCommentRequest;
import com.inversoft.passport.domain.api.UserCommentResponse;
import com.inversoft.passport.domain.api.UserRequest;
import com.inversoft.passport.domain.api.UserResponse;
import com.inversoft.passport.domain.api.email.SendRequest;
import com.inversoft.passport.domain.api.email.SendResponse;
import com.inversoft.passport.domain.api.report.DailyActiveUserReportResponse;
import com.inversoft.passport.domain.api.report.LoginReportResponse;
import com.inversoft.passport.domain.api.report.MonthlyActiveUserReportResponse;
import com.inversoft.passport.domain.api.report.RegistrationReportResponse;
import com.inversoft.passport.domain.api.report.TotalsReportResponse;
import com.inversoft.passport.domain.api.report.UserLoginReportResponse;
import com.inversoft.passport.domain.api.user.ActionRequest;
import com.inversoft.passport.domain.api.user.ActionResponse;
import com.inversoft.passport.domain.api.user.ChangePasswordRequest;
import com.inversoft.passport.domain.api.user.ForgotPasswordRequest;
import com.inversoft.passport.domain.api.user.ImportRequest;
import com.inversoft.passport.domain.api.user.RegistrationRequest;
import com.inversoft.passport.domain.api.user.RegistrationResponse;
import com.inversoft.passport.domain.api.user.SearchResponse;
import com.inversoft.passport.domain.search.AuditLogSearchCriteria;
import com.inversoft.passport.domain.search.UserSearchCriteria;
import com.inversoft.rest.ClientResponse;
import com.inversoft.rest.JSONBodyHandler;
import com.inversoft.rest.JSONResponseHandler;
import com.inversoft.rest.RESTClient;

/**
 * Client that connects to a Passport server and provides access to the full set of Passport APIs.
 *
 * @author Brian Pontarelli
 */
public class PassportClient {
  private final String apiKey;

  private final String baseURL;

  public int connectTimeout = 2000;

  public int readTimeout = 2000;

  public PassportClient(String apiKey, String baseURL) {
    this.apiKey = apiKey;
    this.baseURL = baseURL;
  }

  /**
   * Takes an action on a user. The user being actioned is called the "actionee" and the user taking the action is
   * called the "actioner". Both user ids are required. You pass the actionee's user id into the method and the
   * actioner's is put into the request object.
   *
   * @param actioneeUserId The actionee's user id.
   * @param request        The action request that includes all of the information about the action being taken
   *                       including the id of the action, any options and the duration (if applicable).
   * @return When successful, the response will contain the a notification of the action. If there was a validation
   * error or any other type of error, this will return the Errors object in the response. Additionally, if Passport
   * could not be contacted because it is down or experiencing a failure, the response will contain an Exception, which
   * could be an IOException.
   */
  public ClientResponse<ActionResponse, Errors> actionUser(UUID actioneeUserId, ActionRequest request) {
    return start(ActionResponse.class).uri("/api/user/action")
                                      .urlSegment(actioneeUserId)
                                      .bodyHandler(new JSONBodyHandler(request))
                                      .post()
                                      .go();
  }

  /**
   * Calls the PassportClient and handles all of the exception and error cases.
   * <p>
   * You should call this awesome method like this:
   * <pre>
   *   User user = client.callAPI(() -> client.retrieveUser(userId), (r) -> r.user, this::handleErrors);
   * </pre>
   *
   * @param supplier      The supplier that calls the PassportClient and returns the ClientResponse.
   * @param function      The function that takes the successResponse from the ClientResponse.
   * @param errorConsumer The function that handles the error. You should probably use a method reference to your
   *                      favorite error handling function.
   * @param <T>           The Response type.
   * @param <U>           The successResponse type.
   * @return The result of passing the successResponse to the function.
   */
  public <T, U> U callAPI(Supplier<ClientResponse<T, Errors>> supplier, Function<T, U> function,
                          Consumer<ClientResponse<T, Errors>> errorConsumer) {
    ClientResponse<T, Errors> clientResponse = supplier.get();
    if (!clientResponse.wasSuccessful()) {
      errorConsumer.accept(clientResponse);
      return null;
    }

    return function.apply(clientResponse.successResponse);
  }

  /**
   * Calls the PassportClient for methods that don't have a response or you want to ignore the response. This handles
   * all of the exception and error cases.
   * <p>
   * You should call this awesome method like this:
   * <pre>
   *   User user = client.callNoErrorsAPI(() -> client.retrieveUser(userId), (r) -> r.user);
   * </pre>
   *
   * @param supplier The supplier that calls the PassportClient and returns the ClientResponse.
   * @param function The function that takes the successResponse from the ClientResponse.
   * @param <T>      The Response type.
   * @param <U>      The successResponse type.
   * @return The result of passing the successResponse to the function.
   */
  public <T, U> U callNoErrorAPI(Supplier<ClientResponse<T, Void>> supplier, Function<T, U> function,
                                 Consumer<ClientResponse<T, Void>> errorConsumer) {
    ClientResponse<T, Void> clientResponse = supplier.get();
    if (!clientResponse.wasSuccessful()) {
      errorConsumer.accept(clientResponse);
      return null;
    }

    return function.apply(clientResponse.successResponse);
  }

  /**
   * Calls the PassportClient for methods that don't have a response or you want to ignore the response. This handles
   * all of the exception and error cases.
   * <p>
   * You should call this awesome method like this:
   * <pre>
   *   client.callNoResponseAPI(() -> client.retrieveUser(userId), this::handleErrors);
   * </pre>
   *
   * @param supplier      The supplier that calls the PassportClient and returns the ClientResponse.
   * @param errorConsumer The function that handles the error. You should probably use a method reference to your
   *                      favorite error handling function.
   * @param <T>           The Response type.
   */
  public <T> void callNoResponseAPI(Supplier<ClientResponse<T, Errors>> supplier,
                                    Consumer<ClientResponse<T, Errors>> errorConsumer) {
    ClientResponse<T, Errors> clientResponse = supplier.get();
    if (!clientResponse.wasSuccessful()) {
      errorConsumer.accept(clientResponse);
    }
  }

  /**
   * Cancels the user action.
   *
   * @param actionId The action id of the action to cancel.
   * @param request  The action request that contains the information about the cancellation.
   * @return When successful, the response will contain the a notification of the action. If there was a validation
   * error or any other type of error, this will return the Errors object in the response. Additionally, if Passport
   * could not be contacted because it is down or experiencing a failure, the response will contain an Exception, which
   * could be an IOException.
   */
  public ClientResponse<ActionResponse, Errors> cancelAction(UUID actionId, ActionRequest request) {
    return start(ActionResponse.class).uri("/api/user/action")
                                      .urlSegment(actionId)
                                      .bodyHandler(new JSONBodyHandler(request))
                                      .delete()
                                      .go();
  }

  /**
   * Changes a user's password using the verification id. This usually occurs after an email has been sent to the user
   * and they clicked on a link to reset their password.
   *
   * @param verificationId The verification id used to find the user.
   * @param request        The change password request that contains all of the information used to change the
   *                       password.
   * @return When successful, the response will contains no body, just a status code. If there was a validation error or
   * any other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<Void, Errors> changePassword(String verificationId, ChangePasswordRequest request) {
    return start(Void.TYPE).uri("/api/user/change-password")
                           .urlSegment(verificationId)
                           .bodyHandler(new JSONBodyHandler(request))
                           .post()
                           .go();
  }

  /**
   * Adds a comment to the user's account.
   *
   * @param request The comment request that contains all of the information used to add the comment to the user.
   * @return When successful, the response will not contain a response object, it only contains the status code. If
   * there was a validation error or any other type of error, this will return the Errors object in the response.
   * Additionally, if Passport could not be contacted because it is down or experiencing a failure, the response will
   * contain an Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> commentOnUser(UserCommentRequest request) {
    return start(Void.TYPE).uri("/api/user/comment")
                           .bodyHandler(new JSONBodyHandler(request))
                           .post()
                           .go();
  }

  /**
   * Creates an application. You can optionally specify an id for the application, but this is not required.
   *
   * @param applicationId (Optional) The id to use for the application.
   * @param request       The application request that contains all of the information used to create the application.
   * @return When successful, the response will contains the application object. If there was a validation error or any
   * other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<ApplicationResponse, Errors> createApplication(UUID applicationId, ApplicationRequest request) {
    return start(ApplicationResponse.class).uri("/api/application")
                                           .urlSegment(applicationId)
                                           .bodyHandler(new JSONBodyHandler(request))
                                           .post()
                                           .go();
  }

  /**
   * Creates a new role for an application. You must specify the id of the application you are creating the role for.
   * You can optionally specify an id for the role inside the ApplicationRole object itself, but this is not required.
   *
   * @param applicationId The id of the application to create the role on.
   * @param request       The application request that contains all of the information used to create the role.
   * @return When successful, the response will contains the role object. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<ApplicationResponse, Errors> createApplicationRole(UUID applicationId,
                                                                           ApplicationRequest request) {
    Objects.requireNonNull(applicationId);
    return start(ApplicationResponse.class).uri("/api/application")
                                           .urlSegment(applicationId)
                                           .urlSegment("role")
                                           .bodyHandler(new JSONBodyHandler(request))
                                           .post()
                                           .go();
  }

  /**
   * Creates an audit log with the message and user name (usually an email). Audit logs should be written anytime you
   * make changes to the Passport database. When using the Passport Backend web interface, any changes are automatically
   * written to the audit log. However, if you are accessing the API, you must write the audit logs yourself.
   *
   * @param message    The message for the audit log.
   * @param insertUser The user that took the action being logged.
   * @return When successful, the response will not contain a response but only contains the status code. If there was a
   * validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> createAuditLog(String message, String insertUser) {
    return start(Void.TYPE).uri("/api/system/audit-log")
                           .bodyHandler(new JSONBodyHandler(new AuditLogRequest(new AuditLog(null, insertUser, message))))
                           .post()
                           .go();
  }

  /**
   * Creates an email template. You can optionally specify an id for the email template when calling this method, but it
   * is not required.
   *
   * @param emailTemplateId (Optional) The id for the template.
   * @param request         The email template request that contains all of the information used to create the email
   *                        template.
   * @return When successful, the response will contain the email template object. If there was a validation error or
   * any other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<EmailTemplateResponse, Errors> createEmailTemplate(UUID emailTemplateId,
                                                                           EmailTemplateRequest request) {
    return start(EmailTemplateResponse.class).uri("/api/email/template")
                                             .urlSegment(emailTemplateId)
                                             .bodyHandler(new JSONBodyHandler(request))
                                             .post()
                                             .go();
  }

  /**
   * Creates a notification server. You can optionally specify an id for the notification server when calling this
   * method, but it is not required.
   *
   * @param notificationServerId (Optional) The id for the notification server.
   * @param request              The notification server request that contains all of the information used to create the
   *                             notification server.
   * @return When successful, the response will contain the notification server object. If there was a validation error
   * or any other type of error, this will return the Errors object in the response. Additionally, if Passport could not
   * be contacted because it is down or experiencing a failure, the response will contain an Exception, which could be
   * an IOException.
   */
  public ClientResponse<NotificationServerResponse, Errors> createNotificationServer(UUID notificationServerId,
                                                                                     NotificationServerRequest request) {
    return start(NotificationServerResponse.class).uri("/api/notification-server")
                                                  .urlSegment(notificationServerId)
                                                  .bodyHandler(new JSONBodyHandler(request))
                                                  .post()
                                                  .go();
  }

  /**
   * Creates a user without an id. This will create a random, secure id for the new user.
   *
   * @param request The user request that contains all of the information used to create the user.
   * @return When successful, the response will contain the user object. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserResponse, Errors> createUser(UserRequest request) {
    return createUser(null, request);
  }

  /**
   * Creates a user with an optional id.
   *
   * @param userId  (Optional) The id for the user.
   * @param request The user request that contains all of the information used to create the user.
   * @return When successful, the response will contain the user object. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserResponse, Errors> createUser(UUID userId, UserRequest request) {
    return start(UserResponse.class).uri("/api/user")
                                    .urlSegment(userId)
                                    .bodyHandler(new JSONBodyHandler(request))
                                    .post()
                                    .go();
  }

  /**
   * Creates a user action. This action cannot be taken on a user until this call successfully returns. Anytime after
   * that the user action can be applied to any user.
   *
   * @param userActionId (Optional) The id for the user action.
   * @param request      The user action request that contains all of the information used to create the user action.
   * @return When successful, the response will contain the user action object. If there was a validation error or any
   * other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserActionResponse, Errors> createUserAction(UUID userActionId, UserActionRequest request) {
    return start(UserActionResponse.class).uri("/api/user-action")
                                          .urlSegment(userActionId)
                                          .bodyHandler(new JSONBodyHandler(request))
                                          .post()
                                          .go();
  }

  /**
   * Creates a user reason. This user action reason cannot be used when actioning a user until this call completes
   * successfully. Anytime after that the user action reason can be used.
   *
   * @param request The user action reason request that contains all of the information used to create the user action
   *                reason.
   * @return When successful, the response will contain the user action reason object. If there was a validation error
   * or any other type of error, this will return the Errors object in the response. Additionally, if Passport could not
   * be contacted because it is down or experiencing a failure, the response will contain an Exception, which could be
   * an IOException.
   */
  public ClientResponse<UserActionReasonResponse, Errors> createUserActionReason(UUID userActionReasonId,
                                                                                 UserActionReasonRequest request) {
    return start(UserActionReasonResponse.class).uri("/api/user-action-reason")
                                                .urlSegment(userActionReasonId)
                                                .bodyHandler(new JSONBodyHandler(request))
                                                .post()
                                                .go();
  }

  /**
   * Deactivates the application with the given id.
   *
   * @param applicationId The id of the application to deactivate.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deactivateApplication(UUID applicationId) {
    return start(Void.TYPE).uri("/api/application")
                           .urlSegment(applicationId)
                           .delete()
                           .go();
  }

  /**
   * Deactivates the user with the given id.
   *
   * @param userId The id of the application to deactivate.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deactivateUser(UUID userId) {
    return start(Void.TYPE).uri("/api/user")
                           .urlSegment(userId)
                           .delete()
                           .go();
  }

  /**
   * Deactivates the user action with the given id.
   *
   * @param userActionId The id of the user action to deactivate.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deactivateUserAction(UUID userActionId) {
    return start(Void.TYPE).uri("/api/user-action")
                           .urlSegment(userActionId)
                           .delete()
                           .go();
  }

  /**
   * Hard deletes an application. This is a dangerous operation and should not be used in most circumstances. This will
   * delete the application, any registrations for that application, metrics and reports for the application, all the
   * roles for the application, and any other data associated with the application. This operation could take a very
   * long time, depending on the amount of data in your database.
   *
   * @param applicationId The id of the application to delete.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deleteApplication(UUID applicationId) {
    return start(Void.TYPE).uri("/api/application")
                           .urlSegment(applicationId)
                           .urlParameter("hardDelete", true)
                           .delete()
                           .go();
  }

  /**
   * Hard deletes an application role. This is a dangerous operation and should not be used in most circumstances. This
   * permanently removes the given role from all users that had it.
   *
   * @param applicationId The id of the application that contains the role.
   * @param roleId        The id of the role to delete.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deleteApplicationRole(UUID applicationId, UUID roleId) {
    Objects.requireNonNull(applicationId);
    return start(Void.TYPE).uri("/api/application")
                           .urlSegment(applicationId)
                           .urlSegment("role")
                           .urlSegment(roleId)
                           .delete()
                           .go();
  }

  /**
   * Deletes the email template for the given id.
   *
   * @param emailTemplateId The id of the email template to delete.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deleteEmailTemplate(UUID emailTemplateId) {
    return start(Void.TYPE).uri("/api/email/template")
                           .urlSegment(emailTemplateId)
                           .delete()
                           .go();
  }

  /**
   * Deletes the notification server for the given id.
   *
   * @param notificationServerId The id of the notification server to delete.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deleteNotificationServer(UUID notificationServerId) {
    return start(Void.TYPE).uri("/api/notification-server")
                           .urlSegment(notificationServerId)
                           .delete()
                           .go();
  }

  /**
   * Deletes the user registration for the given user and application.
   *
   * @param userId        The id of the user whose registration is being deleted.
   * @param applicationId The id of the application to remove the registration for.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deleteRegistration(UUID userId, UUID applicationId) {
    Objects.requireNonNull(userId);
    return start(Void.TYPE).uri("/api/user/registration")
                           .urlSegment(userId)
                           .urlSegment(applicationId)
                           .delete()
                           .go();
  }

  /**
   * Deletes the user for the given id. This permanently deletes all information, metrics, reports and data associated
   * with the user.
   *
   * @param userId The id of the user to delete.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deleteUser(UUID userId) {
    return start(Void.TYPE).uri("/api/user")
                           .urlSegment(userId)
                           .urlParameter("hardDelete", true)
                           .delete()
                           .go();
  }

  /**
   * Deletes the user action for the given id. This permanently deletes the user action and also any history and logs of
   * the action being applied to any users.
   *
   * @param userActionId The id of the user action to delete.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deleteUserAction(UUID userActionId) {
    return start(Void.TYPE).uri("/api/user-action")
                           .urlSegment(userActionId)
                           .urlParameter("hardDelete", true)
                           .delete()
                           .go();
  }

  /**
   * Deletes the user action reason for the given id.
   *
   * @param userActionReasonId The id of the user action reason to delete.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> deleteUserActionReason(UUID userActionReasonId) {
    return start(Void.TYPE).uri("/api/user-action-reason")
                           .urlSegment(userActionReasonId)
                           .delete()
                           .go();
  }

  /**
   * Begins the forgot password sequence, which kicks off an email to the user so that they can reset their password.
   *
   * @param request The request that contains the information about the user so that they can be emailed.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> forgotPassword(ForgotPasswordRequest request) {
    return start(Void.TYPE).uri("/api/user/forgot-password")
                           .bodyHandler(new JSONBodyHandler(request))
                           .post()
                           .go();
  }

  /**
   * Bulk imports multiple users. This does some validation, but then tries to run batch inserts of users. This reduces
   * latency when inserting lots of users. Therefore, the error response might contain some information about failures,
   * but it will likely be pretty generic.
   *
   * @param request The request that contains all of the information about all of the users to import.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> importUsers(ImportRequest request) {
    return start(Void.TYPE).uri("/api/user/import")
                           .bodyHandler(new JSONBodyHandler(request))
                           .post()
                           .go();
  }

  /**
   * Logs a user in.
   *
   * @param loginRequest    The login request that contains the user credentials used to log them in.
   * @param callerIPAddress (Optional) The IP address of the end-user that is logging in. If a null value is provided
   *                        the IP address will be that of the client or last proxy that sent the request.
   * @return When successful, the response will contain the user that was logged in. This user object is complete and
   * contains all of the registrations and data for the user. If there was a validation error or any other type of
   * error, this will return the Errors object in the response. Additionally, if Passport could not be contacted because
   * it is down or experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<LoginResponse, Errors> login(LoginRequest loginRequest, String callerIPAddress) {
    return start(LoginResponse.class).uri("/api/login")
                                     .header("X-Forwarded-For", callerIPAddress)
                                     .bodyHandler(new JSONBodyHandler(loginRequest))
                                     .post()
                                     .go();
  }

  /**
   * Sends a ping to Passport indicating that the user was automatically logged into an application. When using
   * Passport's SSO or your own, you should call this if the user is already logged in centrally, but accesses an
   * application where they no longer have a session. This helps correctly track login counts, times and helps with
   * reporting.
   *
   * @param userId          The id of the user that was logged in.
   * @param applicationId   The id of the application that they logged into.
   * @param callerIPAddress (Optional) The IP address of the end-user that is logging in. If a null value is provided
   *                        the IP address will be that of the client or last proxy that sent the request.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> loginPing(UUID userId, UUID applicationId, String callerIPAddress) {
    return start(Void.TYPE).uri("/api/login")
                           .urlSegment(userId)
                           .urlSegment(applicationId)
                           .header("X-Forwarded-For", callerIPAddress)
                           .put()
                           .go();
  }

  /**
   * Modifies a temporal user action by changing the expiration of the action and optionally adding a comment to the
   * action.
   *
   * @param actionId The id of the action to modify. This is technically the user action log id.
   * @param request  The request that contains all of the information about the modification.
   * @return When successful, the response will contain the a notification of the action. If there was a validation
   * error or any other type of error, this will return the Errors object in the response. Additionally, if Passport
   * could not be contacted because it is down or experiencing a failure, the response will contain an Exception, which
   * could be an IOException.
   */
  public ClientResponse<ActionResponse, Errors> modifyAction(UUID actionId, ActionRequest request) {
    return start(ActionResponse.class).uri("/api/user/action")
                                      .urlSegment(actionId)
                                      .bodyHandler(new JSONBodyHandler(request))
                                      .put()
                                      .go();
  }

  /**
   * Reactivates the application for the given id.
   *
   * @param applicationId The id of the application to reactivate.
   * @return When successful, the response will contain the application that was reactivated. If there was a validation
   * error or any other type of error, this will return the Errors object in the response. Additionally, if Passport
   * could not be contacted because it is down or experiencing a failure, the response will contain an Exception, which
   * could be an IOException.
   */
  public ClientResponse<ApplicationResponse, Errors> reactivateApplication(UUID applicationId) {
    return start(ApplicationResponse.class).uri("/api/application")
                                           .urlSegment(applicationId)
                                           .urlParameter("reactivate", true)
                                           .put()
                                           .go();
  }

  /**
   * Reactivates the user action for the given id.
   *
   * @param userId The id of the user action to reactivate.
   * @return When successful, the response will contain the user that was reactivated. If there was a validation error
   * or any other type of error, this will return the Errors object in the response. Additionally, if Passport could not
   * be contacted because it is down or experiencing a failure, the response will contain an Exception, which could be
   * an IOException.
   */
  public ClientResponse<UserResponse, Errors> reactivateUser(UUID userId) {
    return start(UserResponse.class).uri("/api/user")
                                    .urlSegment(userId)
                                    .urlParameter("reactivate", true)
                                    .put()
                                    .go();
  }

  /**
   * Reactivates the user action for the given id.
   *
   * @param userActionId The id of the user action to reactivate.
   * @return When successful, the response will contain the user action that was reactivated. If there was a validation
   * error or any other type of error, this will return the Errors object in the response. Additionally, if Passport
   * could not be contacted because it is down or experiencing a failure, the response will contain an Exception, which
   * could be an IOException.
   */
  public ClientResponse<UserActionResponse, Errors> reactivateUserAction(UUID userActionId) {
    return start(UserActionResponse.class).uri("/api/user-action")
                                          .urlSegment(userActionId)
                                          .urlParameter("reactivate", true)
                                          .put()
                                          .go();
  }

  /**
   * Registers a user for an application. If you provide the User and the UserRegistration object on this request, it
   * will create the user as well as register them for the application. This is called a Full Registration. However, if
   * you only provide the UserRegistration object, then the user must already exist and they will be registered for the
   * application.
   *
   * @param request The request that optionally contains the User and must contain the UserRegistration.
   * @return When successful, the response will contain the UserRegistration and optionally will contain the User if the
   * request as a Full Registration. If there was a validation error or any other type of error, this will return the
   * Errors object in the response. Additionally, if Passport could not be contacted because it is down or experiencing
   * a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<RegistrationResponse, Errors> register(RegistrationRequest request) {
    return register(null, request);
  }

  /**
   * Registers a user for an application. If you provide the User and the UserRegistration object on this request, it
   * will create the user as well as register them for the application. This is called a Full Registration. However, if
   * you only provide the UserRegistration object, then the user must already exist and they will be registered for the
   * application. The user id can also be provided and it will either be used to look up an existing user or it will be
   * used for the newly created User.
   *
   * @param userId  The id of the user being registered for the application and optionally created.
   * @param request The request that optionally contains the User and must contain the UserRegistration.
   * @return When successful, the response will contain the UserRegistration and optionally will contain the User if the
   * request as a Full Registration. If there was a validation error or any other type of error, this will return the
   * Errors object in the response. Additionally, if Passport could not be contacted because it is down or experiencing
   * a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<RegistrationResponse, Errors> register(UUID userId, RegistrationRequest request) {
    return start(RegistrationResponse.class).uri("/api/user/registration")
                                            .urlSegment(userId)
                                            .bodyHandler(new JSONBodyHandler(request))
                                            .post()
                                            .go();
  }

  /**
   * Re-sends the verification email to the user.
   *
   * @param email The email address of the user that needs a new verification email.
   * @return When successful, the response will not contain a response object, it will only contain the status. There
   * are also no errors associated with this request. Additionally, if Passport could not be contacted because it is
   * down or experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<Void, Void> resendEmailVerification(String email) {
    return startVoid(Void.TYPE).uri("/api/user/verify-email")
                               .urlParameter("email", email)
                               .put()
                               .go();
  }

  /**
   * Retrieves a single action for the given id.
   *
   * @param actionId The id of the action to retrieve.
   * @return When successful, the response will contain the the action that was previously taken on a user. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<ActionResponse, Errors> retrieveAction(UUID actionId) {
    return start(ActionResponse.class).uri("/api/user/action")
                                      .urlSegment(actionId)
                                      .get()
                                      .go();
  }

  /**
   * Retrieves all of the actions for the user with the given id.
   *
   * @param userId The id of the user to fetch the actions for.
   * @return When successful, the response will contain all of the user action notifications for the given use. If there
   * was a validation error or any other type of error, this will return the Errors object in the response.
   * Additionally, if Passport could not be contacted because it is down or experiencing a failure, the response will
   * contain an Exception, which could be an IOException.
   */
  public ClientResponse<ActionResponse, Errors> retrieveActions(UUID userId) {
    return start(ActionResponse.class).uri("/api/user/action")
                                      .urlParameter("userId", userId)
                                      .get()
                                      .go();
  }

  /**
   * Retrieves the application for the given id or all of the applications if the id is null.
   *
   * @param applicationId (Optional) The application id.
   * @return When successful, the response will contain the application or applications. There are no errors associated
   * with this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure,
   * the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<ApplicationResponse, Void> retrieveApplication(UUID applicationId) {
    return startVoid(ApplicationResponse.class).uri("/api/application")
                                               .urlSegment(applicationId)
                                               .get()
                                               .go();
  }

  /**
   * Retrieves all of the applications.
   *
   * @return When successful, the response will contain all of the applications. There are no errors associated with
   * this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure, the
   * response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<ApplicationResponse, Void> retrieveApplications() {
    return retrieveApplication(null);
  }

  /**
   * Retrieves the daily active user report between the two instants. If you specify an application id, it will only
   * return the daily active counts for that application.
   *
   * @param applicationId (Optional) The application id.
   * @param start         The start instant as UTC milliseconds since Epoch.
   * @param end           The end instant as UTC milliseconds since Epoch.
   * @return When successful, the response will contain the daily active user counts between the start and end instants.
   * If there was a validation error or any other type of error, this will return the Errors object in the response.
   * Additionally, if Passport could not be contacted because it is down or experiencing a failure, the response will
   * contain an Exception, which could be an IOException.
   */
  public ClientResponse<DailyActiveUserReportResponse, Errors> retrieveDailyActiveReport(UUID applicationId, long start,
                                                                                         long end) {
    return start(DailyActiveUserReportResponse.class).uri("/api/report/daily-active-user")
                                                     .urlParameter("start", start)
                                                     .urlParameter("end", end)
                                                     .urlParameter("applicationId", applicationId)
                                                     .get()
                                                     .go();
  }

  /**
   * Retrieves the email template for the given id. If you don't specify the id, this will return all of the email
   * templates.
   *
   * @param emailTemplateId The id of the email template.
   * @return When successful, the response will contain the email template of the id or all the email templates. There
   * are no errors associated with this request. Additionally, if Passport could not be contacted because it is down or
   * experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<EmailTemplateResponse, Void> retrieveEmailTemplate(UUID emailTemplateId) {
    return startVoid(EmailTemplateResponse.class).uri("/api/email/template")
                                                 .urlSegment(emailTemplateId)
                                                 .get()
                                                 .go();
  }

  /**
   * Creates a preview of the email template provided in the request. This allows you to preview an email template that
   * hasn't been saved to the database yet. The entire email template does not need to be provided on the request. This
   * will create the preview based on whatever is given.
   *
   * @param request The request that contains the email template and optionally a locale to render it in.
   * @return When successful, the response will contain the preview of the email template. If the template was invalid
   * or could not be rendered because of template errors, those will be returned in the Errors object in the response.
   * Additionally, if Passport could not be contacted because it is down or experiencing a failure, the response will
   * contain an Exception, which could be an IOException.
   */
  public ClientResponse<PreviewResponse, Errors> retrieveEmailTemplatePreview(PreviewRequest request) {
    return start(PreviewResponse.class).uri("/api/email/template/preview")
                                       .bodyHandler(new JSONBodyHandler(request))
                                       .post()
                                       .go();
  }

  /**
   * Retrieves all of the email templates.
   *
   * @return When successful, the response will contain all of the email templates. There are no errors associated with
   * this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure, the
   * response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<EmailTemplateResponse, Void> retrieveEmailTemplates() {
    return retrieveEmailTemplate(null);
  }

  /**
   * Retrieves all of the applications that are currently inactive.
   *
   * @return When successful, the response will contain all of the inactive applications. There are no errors associated
   * with this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure,
   * the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<ApplicationResponse, Void> retrieveInactiveApplications() {
    return startVoid(ApplicationResponse.class).uri("/api/application")
                                               .urlParameter("inactive", true)
                                               .get()
                                               .go();
  }

  /**
   * Retrieves all of the user actions that are currently inactive.
   *
   * @return When successful, the response will contain all of the inactive user actions. There are no errors associated
   * with this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure,
   * the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<UserActionResponse, Void> retrieveInactiveUserActions() {
    return startVoid(UserActionResponse.class).uri("/api/user-action")
                                              .urlParameter("inactive", true)
                                              .get()
                                              .go();
  }

  /**
   * Retrieves the login report between the two instants. If you specify an application id, it will only return the
   * login counts for that application.
   *
   * @param applicationId (Optional) The application id.
   * @param start         The start instant as UTC milliseconds since Epoch.
   * @param end           The end instant as UTC milliseconds since Epoch.
   * @return When successful, the response will contain the login counts between the start and end instants. If there
   * was a validation error or any other type of error, this will return the Errors object in the response.
   * Additionally, if Passport could not be contacted because it is down or experiencing a failure, the response will
   * contain an Exception, which could be an IOException.
   */
  public ClientResponse<LoginReportResponse, Errors> retrieveLoginReport(UUID applicationId, long start, long end) {
    return start(LoginReportResponse.class).uri("/api/report/login")
                                           .urlParameter("start", start)
                                           .urlParameter("end", end)
                                           .urlParameter("applicationId", applicationId)
                                           .get()
                                           .go();
  }

  /**
   * Retrieves the monthly active user report between the two instants. If you specify an application id, it will only
   * return the monthly active counts for that application.
   *
   * @param applicationId (Optional) The application id.
   * @param start         The start instant as UTC milliseconds since Epoch.
   * @param end           The end instant as UTC milliseconds since Epoch.
   * @return When successful, the response will contain the monthly active user counts between the start and end
   * instants. If there was a validation error or any other type of error, this will return the Errors object in the
   * response. Additionally, if Passport could not be contacted because it is down or experiencing a failure, the
   * response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<MonthlyActiveUserReportResponse, Errors> retrieveMonthlyActiveReport(UUID applicationId,
                                                                                             long start,
                                                                                             long end) {
    return start(MonthlyActiveUserReportResponse.class).uri("/api/report/monthly-active-user")
                                                       .urlParameter("start", start)
                                                       .urlParameter("end", end)
                                                       .urlParameter("applicationId", applicationId)
                                                       .get()
                                                       .go();
  }

  /**
   * Retrieves the notification server for the given id. If you pass in null for the id, this will return all the
   * notification servers.
   *
   * @param notificationServerId (Optional) The id of the notification server.
   * @return When successful, the response will contain the notification server for the id or all the notification
   * servers. There are no errors associated with this request. Additionally, if Passport could not be contacted because
   * it is down or experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<NotificationServerResponse, Void> retrieveNotificationServer(UUID notificationServerId) {
    return startVoid(NotificationServerResponse.class).uri("/api/notification-server")
                                                      .urlSegment(notificationServerId)
                                                      .get()
                                                      .go();
  }

  /**
   * Retrieves all the notification servers.
   *
   * @return When successful, the response will contain all of the notification servers. There are no errors associated
   * with this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure,
   * the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<NotificationServerResponse, Void> retrieveNotificationServers() {
    return retrieveNotificationServer(null);
  }

  /**
   * Retrieves the user registration for the user with the given id and the given application id.
   *
   * @param userId        The id of the user.
   * @param applicationId The id of the application.
   * @return When successful, the response will contain the user registration object. If there was a validation error or
   * any other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<RegistrationResponse, Errors> retrieveRegistration(UUID userId, UUID applicationId) {
    return start(RegistrationResponse.class).uri("/api/user/registration")
                                            .urlSegment(userId)
                                            .urlSegment(applicationId)
                                            .get()
                                            .go();
  }

  /**
   * Retrieves the registration report between the two instants. If you specify an application id, it will only return
   * the login counts for that application.
   *
   * @param applicationId (Optional) The application id.
   * @param start         The start instant as UTC milliseconds since Epoch.
   * @param end           The end instant as UTC milliseconds since Epoch.
   * @return When successful, the response will contain the registration counts between the start and end instants. If
   * there was a validation error or any other type of error, this will return the Errors object in the response.
   * Additionally, if Passport could not be contacted because it is down or experiencing a failure, the response will
   * contain an Exception, which could be an IOException.
   */
  public ClientResponse<RegistrationReportResponse, Errors> retrieveRegistrationReport(UUID applicationId, long start,
                                                                                       long end) {
    return start(RegistrationReportResponse.class).uri("/api/report/registration")
                                                  .urlParameter("start", start)
                                                  .urlParameter("end", end)
                                                  .urlParameter("applicationId", applicationId)
                                                  .get()
                                                  .go();
  }

  /**
   * Retrieves the system configuration.
   *
   * @return When successful, the response will contain the system configuration. There are no errors associated with
   * this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure, the
   * response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<SystemConfigurationResponse, Void> retrieveSystemConfiguration() {
    return startVoid(SystemConfigurationResponse.class).uri("/api/system-configuration")
                                                       .get()
                                                       .go();
  }

  /**
   * Retrieves the totals report. This contains all of the total counts for each application and the global registration
   * count.
   *
   * @return When successful, the response will contain the total counts for logins and registrations for each
   * application as well as the global registration count. If there was a validation error or any other type of error,
   * this will return the Errors object in the response. Additionally, if Passport could not be contacted because it is
   * down or experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<TotalsReportResponse, Void> retrieveTotalReport() {
    return startVoid(TotalsReportResponse.class).uri("/api/report/totals")
                                                .get()
                                                .go();
  }

  /**
   * Retrieves the user for the given id.
   *
   * @param userId The id of the user.
   * @return When successful, the response will contain the user object. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserResponse, Errors> retrieveUser(UUID userId) {
    return start(UserResponse.class).uri("/api/user")
                                    .urlSegment(userId)
                                    .get()
                                    .go();
  }

  /**
   * Retrieves the user action for the given id. If you pass in null for the id, this will return all of the user
   * actions.
   *
   * @param userActionId (Optional) The id of the user action.
   * @return When successful, the response will contain the user action or all the user actions if null is passed in.
   * There are no errors associated with this request. Additionally, if Passport could not be contacted because it is
   * down or experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<UserActionResponse, Void> retrieveUserAction(UUID userActionId) {
    return startVoid(UserActionResponse.class).uri("/api/user-action")
                                              .urlSegment(userActionId)
                                              .get()
                                              .go();
  }

  /**
   * Retrieves the user action reason for the given id. If you pass in null for the id, this will return all of the user
   * action reasons.
   *
   * @param userActionReasonId (Optional) The id of the user action.
   * @return When successful, the response will contain the user action reason or all the user action reasons if null is
   * passed in. There are no errors associated with this request. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserActionReasonResponse, Void> retrieveUserActionReason(UUID userActionReasonId) {
    return startVoid(UserActionReasonResponse.class).uri("/api/user-action-reason")
                                                    .urlSegment(userActionReasonId)
                                                    .get()
                                                    .go();
  }

  /**
   * Retrieves all the user action reasons.
   *
   * @return When successful, the response will contain all of the user action reasons. There are no errors associated
   * with this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure,
   * the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<UserActionReasonResponse, Void> retrieveUserActionReasons() {
    return retrieveUserActionReason(null);
  }

  /**
   * Retrieves all of the user actions.
   *
   * @return When successful, the response will contain all of the user actions. There are no errors associated with
   * this request. Additionally, if Passport could not be contacted because it is down or experiencing a failure, the
   * response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<UserActionResponse, Void> retrieveUserActions() {
    return retrieveUserAction(null);
  }

  /**
   * Retrieves the user for the given email.
   *
   * @param email The email of the user.
   * @return When successful, the response will contain the user object. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserResponse, Errors> retrieveUserByEmail(String email) {
    return start(UserResponse.class).uri("/api/user")
                                    .urlParameter("email", email)
                                    .get()
                                    .go();
  }

  /**
   * Retrieves the user for the given username.
   *
   * @param username The username of the user.
   * @return When successful, the response will contain the user object. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserResponse, Errors> retrieveUserByUsername(String username) {
    return start(UserResponse.class).uri("/api/user")
                                    .urlParameter("username", username)
                                    .get()
                                    .go();
  }

  /**
   * Retrieves all of the comments for the user with the given id.
   *
   * @param userId The id of the user.
   * @return When successful, the response will contain the comments. If there was a validation error or any other type
   * of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserCommentResponse, Errors> retrieveUserComments(UUID userId) {
    return start(UserCommentResponse.class).uri("/api/user/comment")
                                           .urlSegment(userId)
                                           .get()
                                           .go();
  }

  /**
   * Retrieves the login report between the two instants. If you specify an application id, it will only return the
   * login counts for that application.
   *
   * @param userId The user's id.
   * @param offset The initial record. e.g. 0 is the last login, 100 will be the 100th most recent login.
   * @param limit  (Optional, defaults to 10) The number of records to retrieve.
   * @return When successful, the response will contain RawLogin records. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserLoginReportResponse, Errors> retrieveUserLoginReport(UUID userId, int offset,
                                                                                 Integer limit) {
    return start(UserLoginReportResponse.class).uri("/api/report/user-login")
                                               .urlParameter("userId", userId)
                                               .urlParameter("offset", offset)
                                               .urlParameter("limit", limit != null ? limit : 10)
                                               .get()
                                               .go();
  }

  /**
   * Searches the audit logs with the specified criteria and pagination.
   *
   * @param search The search criteria and pagination information.
   * @return When successful, the response will contain the audit logs that match the criteria and pagination
   * constraints. There are no errors associated with this request. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<AuditLogResponse, Void> searchAuditLogs(AuditLogSearchCriteria search) {
    return startVoid(AuditLogResponse.class).uri("/api/system/audit-log")
                                            .urlParameter("search.user", search.user)
                                            .urlParameter("search.message", search.message)
                                            .urlParameter("search.end", search.end)
                                            .urlParameter("search.start", search.start)
                                            .urlParameter("search.orderBy", search.orderBy)
                                            .urlParameter("search.startRow", search.startRow)
                                            .urlParameter("search.numberOfResults", search.numberOfResults)
                                            .get()
                                            .go();
  }

  /**
   * Retrieves the users for the given ids. If any id is invalid, it is ignored.
   *
   * @param ids The user ids to search for.
   * @return When successful, the response will contain the users that match the ids. If there was a validation error or
   * any other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<SearchResponse, Errors> searchUsers(Collection<UUID> ids) {
    return start(SearchResponse.class).uri("/api/user/search")
                                      .urlParameter("ids", ids)
                                      .get()
                                      .go();
  }

  /**
   * Retrieves the users for the given search criteria and pagination.
   *
   * @param search The search criteria and pagination constraints. Fields used: queryString, numberOfResults, and
   *               startRow
   * @return When successful, the response will contain the users that match the search criteria and pagination
   * constraints. If there was a validation error or any other type of error, this will return the Errors object in the
   * response. Additionally, if Passport could not be contacted because it is down or experiencing a failure, the
   * response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<UserResponse, Errors> searchUsersByQueryString(UserSearchCriteria search) {
    return start(UserResponse.class).uri("/api/user/search")
                                    .urlParameter("queryString", search.queryString)
                                    .urlParameter("numberOfResults", search.numberOfResults)
                                    .urlParameter("startRow", search.startRow)
                                    .get()
                                    .go();
  }

  /**
   * Send an email using an email template id. You can optionally provide <code>requestData</code> to access key value
   * pairs in the email template.
   *
   * @param emailTemplateId The id for the template.
   * @param request         The send email request that contains all of the information used to send the email.
   * @return When successful, the response will not contain a response object but only contains the status. If there was
   * a validation error or any other type of error, this will return the Errors object in the response. Additionally, if
   * Passport could not be contacted because it is down or experiencing a failure, the response will contain an
   * Exception, which could be an IOException.
   */
  public ClientResponse<SendResponse, Errors> sendEmail(UUID emailTemplateId, SendRequest request) {
    return start(SendResponse.class).uri("/api/email/send")
                                    .urlSegment(emailTemplateId)
                                    .bodyHandler(new JSONBodyHandler(request))
                                    .post()
                                    .go();
  }

  /**
   * Updates the application with the given id.
   *
   * @param applicationId The id of the application to update.
   * @param request       The request that contains all of the new application information.
   * @return When successful, the response will contain the application. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<ApplicationResponse, Errors> updateApplication(UUID applicationId, ApplicationRequest request) {
    return start(ApplicationResponse.class).uri("/api/application")
                                           .urlSegment(applicationId)
                                           .bodyHandler(new JSONBodyHandler(request))
                                           .put()
                                           .go();
  }

  /**
   * Updates the application role with the given id for the application.
   *
   * @param applicationId The id of the application that the role belongs to.
   * @param roleId        The id of the role to update.
   * @param request       The request that contains all of the new role information.
   * @return When successful, the response will contain the role. If there was a validation error or any other type of
   * error, this will return the Errors object in the response. Additionally, if Passport could not be contacted because
   * it is down or experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<ApplicationResponse, Errors> updateApplicationRole(UUID applicationId, UUID roleId,
                                                                           ApplicationRequest request) {
    Objects.requireNonNull(applicationId);
    return start(ApplicationResponse.class).uri("/api/application")
                                           .urlSegment(applicationId)
                                           .urlSegment("role")
                                           .urlSegment(roleId)
                                           .bodyHandler(new JSONBodyHandler(request))
                                           .put()
                                           .go();
  }

  /**
   * Updates the email template with the given id.
   *
   * @param emailTemplateId The id of the email template to update.
   * @param request         The request that contains all of the new email template information.
   * @return When successful, the response will contain the email template. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<EmailTemplateResponse, Errors> updateEmailTemplate(UUID emailTemplateId,
                                                                           EmailTemplateRequest request) {
    return start(EmailTemplateResponse.class).uri("/api/email/template")
                                             .urlSegment(emailTemplateId)
                                             .bodyHandler(new JSONBodyHandler(request))
                                             .put()
                                             .go();
  }

  /**
   * Updates the notification server with the given id.
   *
   * @param notificationServerId The id of the notification server to update.
   * @param request              The request that contains all of the new notification server information.
   * @return When successful, the response will contain the notification server. If there was a validation error or any
   * other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<NotificationServerResponse, Errors> updateNotificationServer(UUID notificationServerId,
                                                                                     NotificationServerRequest request) {
    return start(NotificationServerResponse.class).uri("/api/notification-server")
                                                  .urlSegment(notificationServerId)
                                                  .bodyHandler(new JSONBodyHandler(request))
                                                  .put()
                                                  .go();
  }

  /**
   * Updates the registration for the user with the given id and the application defined in the request.
   *
   * @param userId  The id of the user whose registration is going to be updated.
   * @param request The request that contains all of the new registration information.
   * @return When successful, the response will contain the registration. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<RegistrationResponse, Errors> updateRegistration(UUID userId, RegistrationRequest request) {
    Objects.requireNonNull(userId);
    return start(RegistrationResponse.class).uri("/api/user/registration")
                                            .urlSegment(userId)
                                            .bodyHandler(new JSONBodyHandler(request))
                                            .put()
                                            .go();
  }

  /**
   * Updates the system configuration.
   *
   * @param request The request that contains all of the new system configuration information.
   * @return When successful, the response will contain the system configuration. If there was a validation error or any
   * other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<SystemConfigurationResponse, Errors> updateSystemConfiguration(
      SystemConfigurationRequest request) {
    return start(SystemConfigurationResponse.class).uri("/api/system-configuration")
                                                   .bodyHandler(new JSONBodyHandler(request))
                                                   .put()
                                                   .go();
  }

  /**
   * Updates the user with the given id.
   *
   * @param userId  The id of the user to update.
   * @param request The request that contains all of the new user information.
   * @return When successful, the response will contain the user. If there was a validation error or any other type of
   * error, this will return the Errors object in the response. Additionally, if Passport could not be contacted because
   * it is down or experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<UserResponse, Errors> updateUser(UUID userId, UserRequest request) {
    return start(UserResponse.class).uri("/api/user")
                                    .urlSegment(userId)
                                    .bodyHandler(new JSONBodyHandler(request))
                                    .put()
                                    .go();
  }

  /**
   * Updates the user action with the given id.
   *
   * @param userActionId The id of the user action to update.
   * @param request      The request that contains all of the new user action information.
   * @return When successful, the response will contain the user action. If there was a validation error or any other
   * type of error, this will return the Errors object in the response. Additionally, if Passport could not be contacted
   * because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserActionResponse, Errors> updateUserAction(UUID userActionId, UserActionRequest request) {
    return start(UserActionResponse.class).uri("/api/user-action/" + userActionId)
                                          .bodyHandler(new JSONBodyHandler(request))
                                          .put()
                                          .go();
  }

  /**
   * Updates the user action reason with the given id.
   *
   * @param userActionReasonId The id of the user action reason to update.
   * @param request            The request that contains all of the new user action reason information.
   * @return When successful, the response will contain the user action reason. If there was a validation error or any
   * other type of error, this will return the Errors object in the response. Additionally, if Passport could not be
   * contacted because it is down or experiencing a failure, the response will contain an Exception, which could be an
   * IOException.
   */
  public ClientResponse<UserActionReasonResponse, Errors> updateUserActionReason(UUID userActionReasonId,
                                                                                 UserActionReasonRequest request) {
    return start(UserActionReasonResponse.class).uri("/api/user-action-reason")
                                                .urlSegment(userActionReasonId)
                                                .bodyHandler(new JSONBodyHandler(request))
                                                .put()
                                                .go();
  }

  /**
   * Confirms a email verification. The id given is usually from an email sent to the user.
   *
   * @param verificationId The verification id sent to the user.
   * @return When successful, the response will not contain a response object but only contains the status. There are no
   * errors associated with this request. Additionally, if Passport could not be contacted because it is down or
   * experiencing a failure, the response will contain an Exception, which could be an IOException.
   */
  public ClientResponse<Void, Void> verifyEmail(String verificationId) {
    return startVoid(Void.TYPE).uri("/api/user/verify-email")
                               .urlSegment(verificationId)
                               .post()
                               .go();
  }

  /**
   * Confirms a two factor authentication code.
   *
   * @param request The two factor request information.
   * @return When successful, the response will not contain a response object but only contains the status.  If there
   * was a validation error or any other type of error, this will return the Errors object in the response.
   * Additionally, if Passport could not be contacted because it is down or experiencing a failure, the response will
   * contain an Exception, which could be an IOException.
   */
  public ClientResponse<Void, Errors> verifyTwoFactor(TwoFactorRequest request) {
    return start(Void.TYPE).uri("/api/two-factor")
                           .bodyHandler(new JSONBodyHandler(request))
                           .post()
                           .go();
  }

  private <T> RESTClient<T, Errors> start(Class<T> type) {
    return new RESTClient<>(type, Errors.class).authorization(apiKey)
                                               .successResponseHandler(type != Void.TYPE ? new JSONResponseHandler<>(type) : null)
                                               .errorResponseHandler(new JSONResponseHandler<>(Errors.class))
                                               .url(baseURL)
                                               .connectTimeout(connectTimeout)
                                               .readTimeout(readTimeout);
  }

  private <T> RESTClient<T, Void> startVoid(Class<T> type) {
    return new RESTClient<>(type, Void.TYPE).authorization(apiKey)
                                            .successResponseHandler(type != Void.TYPE ? new JSONResponseHandler<>(type) : null)
                                            .url(baseURL)
                                            .connectTimeout(connectTimeout)
                                            .readTimeout(readTimeout);
  }
}
