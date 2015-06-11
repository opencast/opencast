/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adminui.usersettings;

import static org.junit.Assert.assertEquals;

import org.opencastproject.adminui.usersettings.persistence.UserSettingDto;
import org.opencastproject.adminui.usersettings.persistence.UserSettingsServiceException;
import org.opencastproject.comments.persistence.CommentDto;
import org.opencastproject.messages.persistence.MessageSignatureDto;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

public class UserSettingsServiceTest {
  private static final String KEY_PREFIX = "Key-";
  private static final String ORG = "org-1";
  private static final String OTHER_ORG = "org-1";
  private static final String NAME_PREFIX = "Name-";
  private static final String REPLY_TO_PREFIX = "Reply-To-";
  private static final String REPLY_TO_NAME_PREFIX = "Reply-To-Name-";
  private static final String SENDER_PREFIX = "Sender-";
  private static final String SENDER_NAME_PREFIX = "Sender-Name-";
  private static final String SIGNATURE_PREFIX = "Signature-";
  private static final String USER_NAME = "user1";
  private static final String VALUE_PREFIX = "Value-";



  private Date creationDate = new Date();
  private SecurityService securityService;
  private UserDirectoryService userDirectoryService;

  @Before
  public void before() {
    User user = EasyMock.createNiceMock(User.class);
    EasyMock.expect(user.getUsername()).andReturn(USER_NAME);
    EasyMock.replay(user);

    Organization organization = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(organization.getId()).andReturn(ORG).anyTimes();
    EasyMock.replay(organization);

    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(USER_NAME)).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);
  }

  private List<MessageSignatureDto> createSignatureList(int signatureCount) {
    LinkedList<MessageSignatureDto> signatures = new LinkedList<MessageSignatureDto>();
    for (int i = 0; i < signatureCount; i++) {
      MessageSignatureDto messageSignatureDto = new MessageSignatureDto(NAME_PREFIX + i, ORG, USER_NAME, SENDER_PREFIX
              + i, SENDER_NAME_PREFIX + i, REPLY_TO_PREFIX + i, REPLY_TO_PREFIX + 1, SIGNATURE_PREFIX + i,
              creationDate, new LinkedList<CommentDto>());
      signatures.add(messageSignatureDto);
    }
    return signatures;
  }

  private EntityManager setupSignatureEntityManager(int signatureCount, int offset, int limit) {
    Query signatureQuery = EasyMock.createMock(Query.class);
    EasyMock.expect(signatureQuery.setParameter(EasyMock.anyObject(String.class), EasyMock.anyObject())).andReturn(signatureQuery).anyTimes();
    EasyMock.expect(signatureQuery.setFirstResult(offset)).andReturn(signatureQuery).anyTimes();
    EasyMock.expect(signatureQuery.setMaxResults(limit)).andReturn(signatureQuery).anyTimes();
    EasyMock.expect(signatureQuery.getResultList()).andReturn(createSignatureList(signatureCount));
    EasyMock.replay(signatureQuery);

    EntityManager findSignatures = EasyMock.createMock(EntityManager.class);
    EasyMock.expect(findSignatures.createNamedQuery("MessageSignature.findByCreator")).andReturn(signatureQuery);
    findSignatures.close();
    EasyMock.expectLastCall();
    EasyMock.replay(findSignatures);

    return findSignatures;
  }

  private List<UserSettingDto> createUserSettingsList(int settingCount) {
    LinkedList<UserSettingDto> userSettings = new LinkedList<UserSettingDto>();
    for (int i = 0; i < settingCount; i++) {
      UserSettingDto userSettingDto = new UserSettingDto(i, KEY_PREFIX + i, VALUE_PREFIX + i, USER_NAME, ORG);
      userSettings.add(userSettingDto);
    }
    return userSettings;
  }

  private EntityManager setupUserSettingEntityManager(int signatureCount, int offset, int limit) {
    Query userSettingsQuery = EasyMock.createMock(Query.class);
    EasyMock.expect(userSettingsQuery.setParameter(EasyMock.anyObject(String.class), EasyMock.anyObject())).andReturn(userSettingsQuery).anyTimes();
    EasyMock.expect(userSettingsQuery.setFirstResult(offset)).andReturn(userSettingsQuery).anyTimes();
    EasyMock.expect(userSettingsQuery.setMaxResults(limit)).andReturn(userSettingsQuery).anyTimes();
    EasyMock.expect(userSettingsQuery.getResultList()).andReturn(createUserSettingsList(signatureCount));
    EasyMock.replay(userSettingsQuery);

    EntityManager findSettings = EasyMock.createMock(EntityManager.class);
    EasyMock.expect(findSettings.createNamedQuery("UserSettings.findByUserName")).andReturn(userSettingsQuery);
    findSettings.close();
    EasyMock.expectLastCall();
    EasyMock.replay(findSettings);
    return findSettings;
  }

  private EntityManager setupSignatureCountEntityManager(int total, int offset, int limit) {
    Number totalNumber = EasyMock.createMock(Number.class);
    EasyMock.expect(totalNumber.intValue()).andReturn(total);
    EasyMock.replay(totalNumber);

    Query userSettingsQuery = EasyMock.createMock(Query.class);
    EasyMock.expect(userSettingsQuery.setParameter(EasyMock.anyObject(String.class), EasyMock.anyObject())).andReturn(userSettingsQuery).anyTimes();
    EasyMock.expect(userSettingsQuery.getSingleResult()).andReturn(totalNumber);
    EasyMock.replay(userSettingsQuery);

    EntityManager findSettings = EasyMock.createMock(EntityManager.class);
    EasyMock.expect(findSettings.createNamedQuery("MessageSignature.countByCreator")).andReturn(userSettingsQuery);
    findSettings.close();
    EasyMock.expectLastCall();
    EasyMock.replay(findSettings);
    return findSettings;
  }

  private EntityManager setupUserSettingCountEntityManager(int total, int offset, int limit) {
    Number totalNumber = EasyMock.createMock(Number.class);
    EasyMock.expect(totalNumber.intValue()).andReturn(total);
    EasyMock.replay(totalNumber);

    Query userSettingsQuery = EasyMock.createMock(Query.class);
    EasyMock.expect(userSettingsQuery.setParameter(EasyMock.anyObject(String.class), EasyMock.anyObject())).andReturn(userSettingsQuery).anyTimes();
    EasyMock.expect(userSettingsQuery.getSingleResult()).andReturn(totalNumber);
    EasyMock.replay(userSettingsQuery);

    EntityManager findSettings = EasyMock.createMock(EntityManager.class);
    EasyMock.expect(findSettings.createNamedQuery("UserSettings.countByUserName")).andReturn(userSettingsQuery);
    findSettings.close();
    EasyMock.expectLastCall();
    EasyMock.replay(findSettings);
    return findSettings;
  }

  private EntityManagerFactory setupEntityManagerFactory(int settingCount, int signatureCount, int settingTotal, int signatureTotal, int offset, int limit) {
    EntityManagerFactory emf = EasyMock.createMock(EntityManagerFactory.class);
    EasyMock.expect(emf.createEntityManager()).andReturn(setupUserSettingEntityManager(settingCount, offset, limit));
    EasyMock.expect(emf.createEntityManager()).andReturn(setupUserSettingCountEntityManager(settingTotal, offset, limit));
    EasyMock.replay(emf);
    return emf;
  }

  private UserSettingsService setupUserSettingsService(EntityManagerFactory emf) {
    UserSettingsService userSettingsService = new UserSettingsService();
    userSettingsService.setSecurityService(securityService);
    userSettingsService.setEntityManagerFactory(emf);
    userSettingsService.setUserDirectoryService(userDirectoryService);
    return userSettingsService;
  }

  @Test
  public void findUserSettingsInputNoSettingsNoSignaturesExpectsEmptyUserSettings() throws UserSettingsServiceException {
    int offset = 0;
    int limit = 10;
    EntityManagerFactory emf = setupEntityManagerFactory(0, 0, limit, limit, offset, limit);
    UserSettingsService userSettingsService = setupUserSettingsService(emf);
    userSettingsService.findUserSettings(limit, offset);
  }

  @Test
  public void findUserSettingsInputOneSettingsOneSignaturesExpectsProperUserSettings()
          throws UserSettingsServiceException {
    int offset = 0;
    int limit = 1;
    EntityManagerFactory emf = setupEntityManagerFactory(limit, limit, limit, limit, offset, limit);
    UserSettingsService userSettingsService = setupUserSettingsService(emf);
    userSettingsService.findUserSettings(limit, offset);
  }

  @Test
  public void findUserSettingsInputManySettingsManySignaturesExpectsProperUserSettings()
          throws UserSettingsServiceException {
    int offset = 0;
    int limit = 10;
    EntityManagerFactory emf = setupEntityManagerFactory(limit, limit, limit, limit, offset, limit);
    UserSettingsService userSettingsService = setupUserSettingsService(emf);
    userSettingsService.findUserSettings(limit, offset);
  }

  @Test
  public void addUserSettingInputNormalValuesExpectsSavedSetting() throws UserSettingsServiceException {
    String key = "newKey";
    String value = "newValue";
    Capture<UserSettingDto> userSettingDto = new Capture<UserSettingDto>();
    EntityTransaction tx = EasyMock.createNiceMock(EntityTransaction.class);
    EasyMock.replay(tx);

    EntityManager em = EasyMock.createNiceMock(EntityManager.class);
    EasyMock.expect(em.getTransaction()).andReturn(tx);
    em.persist(EasyMock.capture(userSettingDto));
    EasyMock.expectLastCall();
    EasyMock.replay(em);

    EntityManagerFactory emf = EasyMock.createNiceMock(EntityManagerFactory.class);
    EasyMock.expect(emf.createEntityManager()).andReturn(em);
    EasyMock.replay(emf);

    UserSettingsService userSettingsService = new UserSettingsService();
    userSettingsService.setSecurityService(securityService);
    userSettingsService.setEntityManagerFactory(emf);
    userSettingsService.setUserDirectoryService(userDirectoryService);
    userSettingsService.addUserSetting(key, value);

    assertEquals(userSettingDto.getValues().get(0).getKey(), key);
    assertEquals(userSettingDto.getValues().get(0).getValue(), value);
  }

  @Test
  public void updateUserSettingInputNoIdExpectsSavedSetting() throws UserSettingsServiceException {
    long id = 7L;
    String key = "newKey";
    String value = "newValue";
    String oldValue = "oldValue";

    UserSettingDto userSettingDto = new UserSettingDto(id, key, oldValue, securityService.getUser().getUsername(), securityService.getOrganization().getId());
    LinkedList<UserSettingDto> userSettingDtos = new LinkedList<UserSettingDto>();
    userSettingDtos.add(userSettingDto);

    EntityTransaction tx = EasyMock.createNiceMock(EntityTransaction.class);
    EasyMock.replay(tx);

    Query query = EasyMock.createNiceMock(Query.class);
    EasyMock.expect(query.setParameter("key", key)).andReturn(query);
    EasyMock.expect(query.setParameter("username", securityService.getUser().getUsername())).andReturn(query);
    EasyMock.expect(query.setParameter("org", securityService.getOrganization().getId())).andReturn(query);
    EasyMock.expect(query.getResultList()).andReturn(userSettingDtos);
    EasyMock.replay(query);

    EntityManager em = EasyMock.createNiceMock(EntityManager.class);
    EasyMock.expect(em.createNamedQuery("UserSettings.findByKey")).andReturn(query);
    EasyMock.expectLastCall();
    EasyMock.expect(em.getTransaction()).andReturn(tx);
    EasyMock.expect(em.find(UserSettingDto.class, id)).andReturn(userSettingDto);
    EasyMock.replay(em);

    EntityManagerFactory emf = EasyMock.createNiceMock(EntityManagerFactory.class);
    EasyMock.expect(emf.createEntityManager()).andReturn(em).anyTimes();
    EasyMock.replay(emf);

    UserSettingsService userSettingsService = new UserSettingsService();
    userSettingsService.setSecurityService(securityService);
    userSettingsService.setEntityManagerFactory(emf);
    userSettingsService.setUserDirectoryService(userDirectoryService);
    UserSetting result = userSettingsService.updateUserSetting(key, value, oldValue);

    assertEquals(result.getId(), id);
    assertEquals(result.getKey(), key);
    assertEquals(result.getValue(), value);
  }

  @Test
  public void deleteUserSettingInputNoIdExpectsSavedSetting() throws UserSettingsServiceException {
    long id = 8L;
    String key = "newKey";
    String oldValue = "oldValue";

    UserSettingDto userSettingDto = new UserSettingDto(id, key, oldValue, securityService.getUser().getUsername(), securityService.getOrganization().getId());
    LinkedList<UserSettingDto> userSettingDtos = new LinkedList<UserSettingDto>();
    userSettingDtos.add(userSettingDto);

    EntityTransaction tx = EasyMock.createNiceMock(EntityTransaction.class);
    EasyMock.replay(tx);

    Query query = EasyMock.createNiceMock(Query.class);
    EasyMock.expect(query.setParameter("key", key)).andReturn(query);
    EasyMock.expect(query.setParameter("username", securityService.getUser().getUsername())).andReturn(query);
    EasyMock.expect(query.setParameter("org", securityService.getOrganization().getId())).andReturn(query);
    EasyMock.expect(query.getResultList()).andReturn(userSettingDtos);
    EasyMock.replay(query);

    EntityManager em = EasyMock.createNiceMock(EntityManager.class);
    EasyMock.expect(em.getTransaction()).andReturn(tx);
    EasyMock.expect(em.find(UserSettingDto.class, id)).andReturn(userSettingDto);
    em.remove(userSettingDto);
    EasyMock.expectLastCall();
    EasyMock.replay(em);

    EntityManagerFactory emf = EasyMock.createNiceMock(EntityManagerFactory.class);
    EasyMock.expect(emf.createEntityManager()).andReturn(em).anyTimes();
    EasyMock.replay(emf);

    UserSettingsService userSettingsService = new UserSettingsService();
    userSettingsService.setSecurityService(securityService);
    userSettingsService.setEntityManagerFactory(emf);
    userSettingsService.setUserDirectoryService(userDirectoryService);
    userSettingsService.deleteUserSetting(id);
  }
}
