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

package org.opencastproject.messages;

import static org.opencastproject.kernel.mail.EmailAddress.getAddress;
import static org.opencastproject.kernel.mail.EmailAddress.getName;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.comments.Comment;
import org.opencastproject.comments.persistence.CommentDatabaseUtils;
import org.opencastproject.comments.persistence.CommentDto;
import org.opencastproject.kernel.mail.BaseSmtpService;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.persistence.EmailConfigurationDto;
import org.opencastproject.messages.persistence.MailServiceException;
import org.opencastproject.messages.persistence.MessageSignatureDto;
import org.opencastproject.messages.persistence.MessageTemplateDto;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.spi.PersistenceProvider;

/**
 * OSGi service that allows to send e-mails by templates using {@link BaseSmtpService} and implements permanent storage
 * for message templates signatures and the email configuration.
 */
public class MailService {
  public static final String PERSISTENCE_UNIT = "org.opencastproject.messages";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(MailService.class);

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The security service */
  protected SecurityService securityService;

  /** The SMTP service */
  protected final BaseSmtpService smtpService = new BaseSmtpService();

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for participation management");
    emf = persistenceProvider.createEntityManagerFactory(PERSISTENCE_UNIT, persistenceProperties);

    for (Organization org : organizationDirectoryService.getOrganizations()) {
      SecurityUtil.runAs(securityService, org, SecurityUtil.createSystemUser(cc, org), new Effect0() {
        @Override
        protected void run() {
          try {
            updateSmtpConfiguration(getEmailConfiguration());
          } catch (MailServiceException e) {
            logger.error("Unable to initialize the SMTP configuration from the database: {}",
                    ExceptionUtils.getStackTrace(e));
          }
        }
      });
    }
  }

  /** For unit testing purposes. */
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * Closes entity manager factory.
   *
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

  /**
   * OSGi callback to set persistence properties.
   *
   * @param persistenceProperties
   *          persistence properties
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * OSGi callback to set user directory service.
   *
   * @param userDirectoryService
   *          user directory service
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi callback to set persistence provider.
   *
   * @param persistenceProvider
   *          {@link PersistenceProvider} object
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the organization directory service.
   *
   * @param organizationDirectoryService
   *          the organization directory service
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  public List<MessageTemplate> findMessageTemplates(TemplateMessageQuery query) {
    EntityManager em = null;
    List<MessageTemplate> messageTemplates = new ArrayList<MessageTemplate>();

    try {
      em = emf.createEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<MessageTemplateDto> q = cb.createQuery(MessageTemplateDto.class);
      Root<MessageTemplateDto> messageTemplateRoot = q.from(MessageTemplateDto.class);

      List<Predicate> predicates = new ArrayList<Predicate>();

      q.select(messageTemplateRoot);

      String orgId = securityService.getOrganization().getId();
      predicates.add(cb.equal(messageTemplateRoot.get("organization"), orgId));

      if (!query.isIncludeHidden())
        predicates.add(cb.isFalse(messageTemplateRoot.get("hidden").as(Boolean.class)));

      if (StringUtils.isNotEmpty(query.getName()))
        predicates.add(cb.equal(messageTemplateRoot.get("name"), query.getName()));

      if (StringUtils.isNotEmpty(query.getCreator()))
        predicates.add(cb.equal(messageTemplateRoot.get("creator"), query.getCreator()));

      if (query.getType() != null)
        predicates.add(cb.equal(messageTemplateRoot.get("type").as(TemplateType.Type.class), query.getType()));

      if (StringUtils.isNotEmpty(query.getFullText())) {
        List<Predicate> fullTextPredicates = new ArrayList<Predicate>();
        fullTextPredicates.add(cb.like(messageTemplateRoot.<String> get("name"), "%" + query.getFullText() + "%"));
        fullTextPredicates.add(cb.like(messageTemplateRoot.<String> get("subject"), "%" + query.getFullText() + "%"));
        fullTextPredicates.add(cb.like(messageTemplateRoot.<String> get("body"), "%" + query.getFullText() + "%"));
        predicates.add(cb.or(fullTextPredicates.toArray(new Predicate[fullTextPredicates.size()])));
      }

      q.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

      TypedQuery<MessageTemplateDto> typedQuery = em.createQuery(q);
      List<MessageTemplateDto> messageTemplatesDto = typedQuery.getResultList();

      for (MessageTemplateDto mt : messageTemplatesDto) {
        messageTemplates.add(mt.toMessageTemplate(userDirectoryService));
      }

      return messageTemplates;
    } finally {
      if (em != null)
        em.close();
    }
  }

  public MessageTemplate getMessageTemplate(Long id) throws MailServiceException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      Option<MessageTemplateDto> template = findMessageTemplateById(id, orgId, em);
      if (template.isNone())
        throw new NotFoundException();

      return template.get().toMessageTemplate(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get message template {}: {}", id, e.getMessage());
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public List<MessageTemplate> getMessageTemplates() throws MailServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      TypedQuery<MessageTemplateDto> q = em.createNamedQuery("MessageTemplate.findAll", MessageTemplateDto.class).setParameter("org", orgId);
      List<MessageTemplate> templates = new ArrayList<MessageTemplate>();
      List<MessageTemplateDto> result = q.getResultList();
      for (MessageTemplateDto m : result) {
        templates.add(m.toMessageTemplate(userDirectoryService));
      }
      return templates;
    } catch (Exception e) {
      logger.error("Could not get message templates: {}", ExceptionUtils.getStackTrace(e));
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public List<MessageTemplate> getMessageTemplateByName(String messageTemplateName) throws MailServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      TypedQuery<MessageTemplateDto> q = em.createNamedQuery("MessageTemplate.findByName", MessageTemplateDto.class)
              .setParameter("org", orgId);
      q.setParameter("name", messageTemplateName);
      List<MessageTemplate> templates = new ArrayList<MessageTemplate>();
      List<MessageTemplateDto> result = q.getResultList();
      for (MessageTemplateDto m : result) {
        templates.add(m.toMessageTemplate(userDirectoryService));
      }
      return templates;
    } catch (Exception e) {
      logger.error("Could not get message templates: {}", ExceptionUtils.getStackTrace(e));
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public List<MessageTemplate> getMessageTemplatesStartingWith(String filterText) throws MailServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      TypedQuery<MessageTemplateDto> q = em.createNamedQuery("MessageTemplate.likeName", MessageTemplateDto.class)
              .setParameter("org", orgId);
      q.setParameter("name", filterText + "%");
      List<MessageTemplate> templates = new ArrayList<MessageTemplate>();
      List<MessageTemplateDto> result = q.getResultList();
      for (MessageTemplateDto m : result) {
        templates.add(m.toMessageTemplate(userDirectoryService));
      }
      return templates;
    } catch (Exception e) {
      logger.error("Could not get message templates: {}", ExceptionUtils.getStackTrace(e));
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public MessageTemplate updateMessageTemplate(MessageTemplate template) throws MailServiceException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String orgId = securityService.getOrganization().getId();
      MessageTemplateDto msgTmpl = mergeMessageTemplate(template, orgId, em);
      tx.commit();
      return msgTmpl.toMessageTemplate(userDirectoryService);
    } catch (Exception e) {
      logger.error("Could not update message template '{}': {}", template, e.getMessage());
      if (tx.isActive())
        tx.rollback();
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public void deleteMessageTemplate(Long id) throws MailServiceException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String orgId = securityService.getOrganization().getId();
      Option<MessageTemplateDto> templateOption = findMessageTemplateById(id, orgId, em);
      if (templateOption.isNone())
        throw new NotFoundException();
      em.remove(templateOption.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete message template '{}': {}", id, e.getMessage());
      if (tx.isActive())
        tx.rollback();
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public List<MessageSignature> getMessageSignatures() throws MailServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      Query q = em.createNamedQuery("MessageSignature.findAll").setParameter("org", orgId);
      List<MessageSignature> signatures = new ArrayList<MessageSignature>();
      List<MessageSignatureDto> result = q.getResultList();
      for (MessageSignatureDto m : result) {
        signatures.add(m.toMessageSignature(userDirectoryService));
      }
      return signatures;
    } catch (Exception e) {
      logger.error("Could not get message signatures: {}", e.getMessage());
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

   /**
    * Get all of the message signatures for the current user.
    * @return A list of all of the message signatures.
    * @throws UserSettingsServiceException
    */
   @SuppressWarnings("unchecked")
  public List<MessageSignature> getMessageSignaturesByUserName() throws MailServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      String username = securityService.getUser().getUsername();
      Query q = em.createNamedQuery("MessageSignature.findByCreator").setParameter("username", username).setParameter("org", orgId);
      List<MessageSignature> signatures = new ArrayList<MessageSignature>();
      List<MessageSignatureDto> result = q.getResultList();
      for (MessageSignatureDto m : result) {
        signatures.add(m.toMessageSignature(userDirectoryService));
      }
      return signatures;
     } catch (Exception e) {
       logger.error("Could not get message signatures: {}", ExceptionUtils.getStackTrace(e));
       throw new MailServiceException(e);
     } finally {
         if (em != null) {
            em.close();
         }
     }
  }


  /**
   * Get the current logged in user's signature
   * @return The message signature
   * @throws UserSettingsServiceException
   */
  public MessageSignature getCurrentUsersSignature() throws MailServiceException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      String username = securityService.getUser().getUsername();
      TypedQuery<MessageSignatureDto> q = em.createNamedQuery("MessageSignature.findByCreator", MessageSignatureDto.class).setParameter("username", username).setParameter("org", orgId);
      MessageSignatureDto messageSignatureDto = q.getSingleResult();
      return messageSignatureDto.toMessageSignature(userDirectoryService);
    } catch (NoResultException e) {
        throw new NotFoundException(e);
    } catch (Exception e) {
      logger.error("Could not get message signatures: {}", ExceptionUtils.getStackTrace(e));
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * @return Finds the total number of message signatures for the current user.
   * @throws UserSettingsServiceException
   */
  public int getSignatureTotalByUserName() throws MailServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      String username = securityService.getUser().getUsername();
      Query q = em.createNamedQuery("MessageSignature.countByCreator").setParameter("username", username)
              .setParameter("org", orgId);
      Number countResult = (Number) q.getSingleResult();
      return countResult.intValue();
    } catch (Exception e) {
      logger.error("Could not count message signatures: {}", ExceptionUtils.getStackTrace(e));
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public MessageSignature getMessageSignature(Long id) throws MailServiceException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      Option<MessageSignatureDto> signature = findMessageSignatureById(id, orgId, em);
      if (signature.isNone())
        throw new NotFoundException();

      return signature.get().toMessageSignature(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get message signature {}: {}", id, e.getMessage());
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public MessageSignature updateMessageSignature(MessageSignature signature) throws MailServiceException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String orgId = securityService.getOrganization().getId();
      MessageSignatureDto msgSign = mergeMessageSignature(signature, orgId, em);
      tx.commit();
      return msgSign.toMessageSignature(userDirectoryService);
    } catch (Exception e) {
      logger.error("Could not update message signature '{}': {}", signature, e.getMessage());
      if (tx.isActive())
        tx.rollback();
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public void deleteMessageSignature(Long id) throws MailServiceException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String orgId = securityService.getOrganization().getId();
      Option<MessageSignatureDto> signatureOption = findMessageSignatureById(id, orgId, em);
      if (signatureOption.isNone())
        throw new NotFoundException();
      em.remove(signatureOption.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete message signature '{}': {}", id, e.getMessage());
      if (tx.isActive())
        tx.rollback();
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public EmailConfiguration getEmailConfiguration() throws MailServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      Option<EmailConfigurationDto> emailOption = findEmailConfiguration(orgId, em);
      if (emailOption.isSome()) {
        return emailOption.get().toEmailConfiguration();
      } else {
        return EmailConfiguration.DEFAULT;
      }
    } catch (Exception e) {
      logger.error("Could not get email configuration: {}", e.getMessage());
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public EmailConfiguration updateEmailConfiguration(EmailConfiguration emailConfiguration) throws MailServiceException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String orgId = securityService.getOrganization().getId();
      EmailConfigurationDto emailConfig = mergeEmailConfiguration(emailConfiguration, orgId, em);
      tx.commit();
      EmailConfiguration updatedEmailConfiguration = emailConfig.toEmailConfiguration();
      updateSmtpConfiguration(updatedEmailConfiguration);
      return updatedEmailConfiguration;
    } catch (Exception e) {
      logger.error("Could not update email configuration '{}': {}", emailConfiguration, e.getMessage());
      if (tx.isActive())
        tx.rollback();
      throw new MailServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public void send(Mail mail) throws MailServiceException {
    MimeMessage mimeMessage;
    try {
      mimeMessage = toMimeMessage(mail);
    } catch (Exception e) {
      logger.error("Unable to create mime message: {}", ExceptionUtils.getStackTrace(e));
      throw new MailServiceException(e);
    }
    try {
      smtpService.send(mimeMessage);
    } catch (MessagingException e) {
      logger.error("Unable to send the email: {}", ExceptionUtils.getStackTrace(e));
      throw new MailServiceException(e);
    }
  }

  public static MessageTemplateDto mergeMessageTemplate(MessageTemplate template, String organization, EntityManager em) {
    ArrayList<CommentDto> comments = new ArrayList<CommentDto>();
    for (Comment c : template.getComments()) {
      comments.add(CommentDatabaseUtils.mergeComment(c, em));
    }

    Option<MessageTemplateDto> dtoOption = findMessageTemplate(option(template.getId()), template.getName(),
            organization, em);
    MessageTemplateDto dto;
    if (dtoOption.isSome()) {
      dto = dtoOption.get();
      dto.setName(template.getName());
      dto.setType(template.getType().getType());
      dto.setCreator(template.getCreator().getUsername());
      dto.setSubject(template.getSubject());
      dto.setBody(template.getBody());
      dto.setCreationDate(template.getCreationDate());
      dto.setComments(comments);
      dto.setHidden(template.isHidden());
      em.merge(dto);
    } else {
      dto = new MessageTemplateDto(template.getName(), organization, template.getCreator().getUsername(),
              template.getSubject(), template.getBody(), template.getType().getType(), template.getCreationDate(),
              comments);
      dto.setHidden(template.isHidden());
      em.persist(dto);
    }
    return dto;
  }

  public static Option<EmailConfigurationDto> findEmailConfiguration(String organization, EntityManager em) {
    Query q = em.createNamedQuery("EmailConfiguration.findAll").setParameter("org", organization).setMaxResults(1);
    try {
      return some((EmailConfigurationDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static EmailConfigurationDto mergeEmailConfiguration(EmailConfiguration config, String organization,
          EntityManager em) {
    Option<EmailConfigurationDto> configOption = findEmailConfiguration(organization, em);
    EmailConfigurationDto dto;
    if (configOption.isSome()) {
      dto = configOption.get();
      dto.setTransport(config.getTransport());
      dto.setPassword(config.getPassword());
      dto.setPort(config.getPort());
      dto.setServer(config.getServer());
      dto.setSsl(config.isSsl());
      dto.setUserName(config.getUserName());
      em.merge(dto);
    } else {
      dto = new EmailConfigurationDto(organization, config.getTransport(), config.getServer(), config.getPort(),
              config.getUserName(), config.getPassword(), config.isSsl());
      em.persist(dto);
    }
    return dto;
  }

  public static MessageSignatureDto mergeMessageSignature(MessageSignature signature, String organization,
          EntityManager em) {
    ArrayList<CommentDto> comments = new ArrayList<CommentDto>();
    for (Comment c : signature.getComments()) {
      comments.add(CommentDatabaseUtils.mergeComment(c, em));
    }

    Option<MessageSignatureDto> signatureOption = findMessageSignature(option(signature.getId()), signature.getName(),
            organization, em);
    MessageSignatureDto dto;
    if (signatureOption.isSome()) {
      dto = signatureOption.get();
      dto.setComments(comments);
      dto.setCreationDate(signature.getCreationDate());
      dto.setCreator(signature.getCreator().getUsername());
      dto.setSender(signature.getSender().getAddress());
      dto.setSenderName(signature.getSender().getName());
      dto.setReplyTo(signature.getReplyTo().map(getAddress).getOrElseNull());
      dto.setReplyToName(signature.getReplyTo().map(getName).getOrElseNull());
      dto.setName(signature.getName());
      dto.setSignature(signature.getSignature());
      em.merge(dto);
    } else {
      dto = new MessageSignatureDto(signature.getName(), organization, signature.getCreator().getUsername(), signature
              .getSender().getAddress(), signature.getSender().getName(), signature.getReplyTo().map(getAddress)
              .getOrElseNull(), signature.getReplyTo().map(getName).getOrElseNull(), signature.getSignature(),
              signature.getCreationDate(), comments);
      em.persist(dto);
    }
    return dto;
  }

  public static Option<MessageTemplateDto> findMessageTemplate(Option<Long> id, String name, String orgId,
          EntityManager em) {
    Option<MessageTemplateDto> messageTemplateDto = Option.<MessageTemplateDto> none();
    if (id.isSome())
      messageTemplateDto = findMessageTemplateById(id.get(), orgId, em);

    if (messageTemplateDto.isSome())
      return messageTemplateDto;

    Query q = em.createNamedQuery("MessageTemplate.findByName").setParameter("name", name).setParameter("org", orgId);
    try {
      return some((MessageTemplateDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static Option<MessageTemplateDto> findMessageTemplateById(long id, String orgId, EntityManager em) {
    Query q = em.createNamedQuery("MessageTemplate.findById").setParameter("id", id).setParameter("org", orgId);
    try {
      return some((MessageTemplateDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static Option<MessageSignatureDto> findMessageSignature(Option<Long> id, String name, String orgId,
          EntityManager em) {
    Option<MessageSignatureDto> messageSignatureDto = Option.<MessageSignatureDto> none();
    if (id.isSome())
      messageSignatureDto = findMessageSignatureById(id.get(), orgId, em);

    if (messageSignatureDto.isSome())
      return messageSignatureDto;

    Query q = em.createNamedQuery("MessageSignature.findByName").setParameter("name", name).setParameter("org", orgId);
    try {
      return some((MessageSignatureDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static Option<MessageSignatureDto> findMessageSignatureById(long id, String orgId, EntityManager em) {
    Query q = em.createNamedQuery("MessageSignature.findById").setParameter("id", id).setParameter("org", orgId);
    try {
      return some((MessageSignatureDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  /**
   * Find the entity from the given type with the given id
   *
   * @param id
   *          the identifier of the entity to find
   * @param em
   *          The entity manager
   * @param entityClass
   *          The class of the type to find
   * @return an {@link org.opencastproject.util.data.Option option} object
   */
  public static <A> Option<A> find(Option<Long> id, EntityManager em, Class<A> entityClass) {
    for (Long a : id) {
      return option(em.find(entityClass, a));
    }
    return none();
  }

  /** Message -> MimeMessage. */
  private MimeMessage toMimeMessage(Mail mail) throws Exception {
    final MimeMessage msg = smtpService.createMessage();
    for (EmailAddress reply : mail.getReplyTo())
      msg.setReplyTo(new Address[] { new InternetAddress(reply.getAddress(), reply.getName(), "UTF-8") });

    // recipient
    for (EmailAddress recipient : mail.getRecipients()) {
      msg.addRecipient(javax.mail.Message.RecipientType.TO,
              new InternetAddress(recipient.getAddress(), recipient.getName(), "UTF-8"));
    }

    // subject
    msg.setSubject(mail.getSubject());

    EmailAddress from = mail.getSender();
    msg.setFrom(new InternetAddress(from.getAddress(), from.getName(), "UTF-8"));
    // body
    msg.setText(mail.getBody(), "UTF-8");
    return msg;
  }

  private void updateSmtpConfiguration(EmailConfiguration emailConfiguration) {
    smtpService.setMailTransport(emailConfiguration.getTransport());
    smtpService.setHost(emailConfiguration.getServer());
    smtpService.setPort(emailConfiguration.getPort());
    smtpService.setUser(emailConfiguration.getUserName());
    smtpService.setPassword(emailConfiguration.getPassword());
    smtpService.setSsl(emailConfiguration.isSsl());
    smtpService.setDebug(false);
    smtpService.setSender(null);
    smtpService.configure();
  }
}
