package com.barboya.orders;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    Optional<CustomerOrder> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    Page<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<CustomerOrder> findByBusinessIdAndStatusInOrderByCreatedAtDesc(
            UUID businessId, Collection<OrderStatus> statuses, Pageable pageable);

    List<CustomerOrder> findByFulfillmentAndCourierIdIsNullAndStatusIn(
            CustomerOrder.Fulfillment fulfillment, Collection<OrderStatus> statuses);

    Optional<CustomerOrder> findFirstByCourierIdAndStatusIn(UUID courierId, Collection<OrderStatus> statuses);

    Page<CustomerOrder> findByCourierIdAndStatusInOrderByCreatedAtDesc(
            UUID courierId, Collection<OrderStatus> statuses, Pageable pageable);

    @Query("""
            select coalesce(sum(o.deliveryFeeCop - o.courierCommissionCop), 0) from CustomerOrder o
            where o.courierId = :courierId and o.status = com.barboya.orders.OrderStatus.DELIVERED
              and o.deliveredAt >= :since""")
    long earningsSince(@Param("courierId") UUID courierId, @Param("since") Instant since);

    @Query("select o.id from CustomerOrder o where o.status = :status and o.createdAt < :before "
            + "order by o.createdAt")
    List<UUID> findIdsCreatedBefore(@Param("status") OrderStatus status, @Param("before") Instant before,
                                    Pageable pageable);

    Page<CustomerOrder> findByNumber(Long number, Pageable pageable);

    Page<CustomerOrder> findByStatusIn(Collection<OrderStatus> statuses, Pageable pageable);

    /** Bloqueado: dos acciones simultaneas sobre un pedido se aplican en orden. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> lockById(@Param("id") UUID id);
}
