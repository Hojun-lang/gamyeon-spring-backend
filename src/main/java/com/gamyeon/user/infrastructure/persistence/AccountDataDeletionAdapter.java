package com.gamyeon.user.infrastructure.persistence;

import com.gamyeon.user.application.port.outbound.AccountDataDeletionPort;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountDataDeletionAdapter implements AccountDataDeletionPort {

  private final EntityManager entityManager;

  @Override
  public List<String> findFileKeys(Long userId) {
    List<Long> intvIds = findIntvIds(userId);
    if (intvIds.isEmpty()) {
      return List.of();
    }

    List<Long> preparationIds =
        entityManager
            .createQuery("select p.id from Preparation p where p.intvId in :intvIds", Long.class)
            .setParameter("intvIds", intvIds)
            .getResultList();

    List<String> fileKeys = new ArrayList<>();
    if (!preparationIds.isEmpty()) {
      fileKeys.addAll(
          entityManager
              .createQuery(
                  "select pf.fileKey from PreparationFile pf where pf.preparationId in :preparationIds",
                  String.class)
              .setParameter("preparationIds", preparationIds)
              .getResultList());
    }
    fileKeys.addAll(
        entityManager
            .createQuery("select a.fileKey from Answer a where a.intvId in :intvIds", String.class)
            .setParameter("intvIds", intvIds)
            .getResultList());
    return fileKeys;
  }

  @Override
  public void deleteAllByUserId(Long userId) {
    List<Long> intvIds = findIntvIds(userId);
    if (!intvIds.isEmpty()) {
      List<Long> preparationIds =
          entityManager
              .createQuery("select p.id from Preparation p where p.intvId in :intvIds", Long.class)
              .setParameter("intvIds", intvIds)
              .getResultList();
      List<Long> answerIds =
          entityManager
              .createQuery("select a.id from Answer a where a.intvId in :intvIds", Long.class)
              .setParameter("intvIds", intvIds)
              .getResultList();
      List<Long> questionSetIds =
          entityManager
              .createQuery("select q.id from QuestionSet q where q.intvId in :intvIds", Long.class)
              .setParameter("intvIds", intvIds)
              .getResultList();

      if (!answerIds.isEmpty()) {
        execute("delete from AnswerAnalysisJob j where j.answerId in :ids", "ids", answerIds);
      }
      if (!questionSetIds.isEmpty()) {
        execute(
            "delete from FeedbackCallbackJob j where j.questionSetId in :ids",
            "ids",
            questionSetIds);
      }
      execute("delete from FeedbackCallbackJob j where j.intvId in :ids", "ids", intvIds);
      execute("delete from FeedbackEntity f where f.intvId in :ids", "ids", intvIds);
      execute("delete from Answer a where a.intvId in :ids", "ids", intvIds);
      execute("delete from QuestionSet q where q.intvId in :ids", "ids", intvIds);
      if (!preparationIds.isEmpty()) {
        execute(
            "delete from PreparationFile f where f.preparationId in :ids", "ids", preparationIds);
      }
      execute("delete from Preparation p where p.intvId in :ids", "ids", intvIds);
      execute("delete from Report r where r.intvId in :ids", "ids", intvIds);
      execute("delete from Notif n where n.intvId in :ids", "ids", intvIds);
      execute("delete from Intv i where i.id in :ids", "ids", intvIds);
    }

    entityManager
        .createQuery("delete from Report r where r.userId = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager
        .createQuery("delete from Notif n where n.userId = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager
        .createQuery("delete from RefreshToken r where r.userId = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
  }

  private List<Long> findIntvIds(Long userId) {
    return entityManager
        .createQuery("select i.id from Intv i where i.userId = :userId", Long.class)
        .setParameter("userId", userId)
        .getResultList();
  }

  private void execute(String jpql, String parameterName, List<Long> ids) {
    entityManager.createQuery(jpql).setParameter(parameterName, ids).executeUpdate();
  }
}
