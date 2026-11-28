package com.barboya.ratings;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    /** Promedio y cantidad de las calificaciones visibles de cada destinatario. */
    interface Totals {
        UUID getTargetId();

        double getAverage();

        long getCount();
    }

    @Query("select r.targetId as targetId, avg(r.stars) as average, count(r) as count from Rating r "
            + "where r.targetType = :type and r.targetId in :ids and r.hidden = false group by r.targetId")
    List<Totals> totals(@Param("type") Rating.Target type, @Param("ids") Collection<UUID> ids);

    List<Rating> findByOrderId(UUID orderId);

    Page<Rating> findByTargetTypeAndTargetIdAndHiddenFalse(Rating.Target type, UUID targetId, Pageable pageable);

    Page<Rating> findByTargetTypeAndTargetIdAndHiddenFalseAndCommentNotNull(
            Rating.Target type, UUID targetId, Pageable pageable);

    Page<Rating> findByHidden(boolean hidden, Pageable pageable);
}
