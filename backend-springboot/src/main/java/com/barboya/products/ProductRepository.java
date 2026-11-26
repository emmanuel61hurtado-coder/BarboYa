package com.barboya.products;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByBusinessIdAndArchivedFalseOrderByCategoryAscNameAsc(UUID businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.businessId = :businessId and p.archived = false")
    Optional<Product> lockOwned(@Param("id") UUID id, @Param("businessId") UUID businessId);

    /** Siempre en el mismo orden para que dos pedidos no se bloqueen entre si. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    List<Product> lockAll(@Param("ids") Collection<UUID> ids);
}
