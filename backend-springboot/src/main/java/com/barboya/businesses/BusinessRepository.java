package com.barboya.businesses;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByOwnerId(UUID ownerId);

    /**
     * Solo comercios cuya cuenta fue aprobada. El texto, ya en minusculas y con
     * comodines, se busca en el nombre del comercio o de sus productos.
     */
    @Query("""
            select b from Business b
            where exists (select 1 from User u where u.id = b.ownerId
                          and u.status = com.barboya.users.UserStatus.ACTIVE)
              and (:category is null or b.category = :category)
              and (lower(b.name) like :pattern escape '!'
                   or exists (select 1 from Product p where p.businessId = b.id and p.archived = false
                              and p.available = true and lower(p.name) like :pattern escape '!'))
            order by b.acceptingOrders desc, b.name""")
    Page<Business> search(@Param("pattern") String pattern, @Param("category") Business.Category category,
                          Pageable pageable);
}
