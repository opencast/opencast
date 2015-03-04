/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.comments.persistence;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.comments.Comment;
import org.opencastproject.comments.CommentReply;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

/** Utility class for user directory persistence methods */
public final class CommentDatabaseUtils {

  private CommentDatabaseUtils() {
  }

  public static CommentDto mergeComment(Comment comment, EntityManager em) {
    ArrayList<CommentReplyDto> replies = new ArrayList<CommentReplyDto>();
    for (CommentReply c : comment.getReplies()) {
      replies.add(CommentDatabaseUtils.mergeCommentReply(c, em));
    }

    Option<CommentDto> commentOption;
    commentOption = find(comment.getId(), em, CommentDto.class);
    CommentDto dto;
    if (commentOption.isSome()) {
      dto = commentOption.get();
      dto.setText(comment.getText());
      dto.setModificationDate(comment.getModificationDate());
      dto.setReason(comment.getReason());
      dto.setResolvedStatus(comment.isResolvedStatus());
      deleteReplies(dto.getReplies(), replies, em);
      dto.setReplies(replies);
      em.merge(dto);
    } else {
      dto = new CommentDto(comment.getText());
      dto.setModificationDate(comment.getModificationDate());
      dto.setCreationDate(comment.getCreationDate());
      dto.setReason(comment.getReason());
      dto.setResolvedStatus(comment.isResolvedStatus());
      dto.setReplies(replies);
      dto.setAuthor(comment.getAuthor().getUsername());
      em.persist(dto);
    }
    return dto;
  }

  public static CommentReplyDto mergeCommentReply(CommentReply reply, EntityManager em) {
    Option<CommentReplyDto> replyOption;
    replyOption = find(reply.getId(), em, CommentReplyDto.class);
    CommentReplyDto dto;
    if (replyOption.isSome()) {
      dto = replyOption.get();
      dto.setText(reply.getText());
      dto.setModificationDate(reply.getModificationDate());
      em.merge(dto);
    } else {
      dto = new CommentReplyDto(reply.getText());
      dto.setAuthor(reply.getAuthor().getUsername());
      dto.setCreationDate(reply.getCreationDate());
      dto.setModificationDate(reply.getModificationDate());
      em.persist(dto);
    }
    return dto;
  }

  private static Function2<Option<CommentReplyDto>, CommentReplyDto, Boolean> contains = new Function2<Option<CommentReplyDto>, CommentReplyDto, Boolean>() {
    @Override
    public Boolean apply(Option<CommentReplyDto> toDelete, CommentReplyDto toMatch) {
      if (toDelete.isNone())
        return true;
      return toDelete.get().getId() == toMatch.getId();
    }
  };

  public static void deleteReplies(List<CommentReplyDto> oldReplies, List<CommentReplyDto> newReplies, EntityManager em) {
    for (CommentReplyDto p : oldReplies) {
      Option<CommentReplyDto> replyOption = find(option(p.getId()), em, CommentReplyDto.class);
      boolean remain = mlist(newReplies).exists(contains.curry(replyOption));
      if (!remain) {
        em.remove(replyOption.get());
      }
    }
  }

  public static void deleteReplies(List<CommentReplyDto> replies, EntityManager em) {
    for (CommentReplyDto p : replies) {
      Option<CommentReplyDto> replyOption = find(option(p.getId()), em, CommentReplyDto.class);
      if (replyOption.isSome()) {
        em.remove(replyOption.get());
      }
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

}
