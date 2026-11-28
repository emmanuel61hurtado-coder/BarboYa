package com.barboya.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByPartyIdOrderByCreatedAtDesc(UUID partyId, Pageable pageable);

    boolean existsByReversalOf(UUID entryId);

    List<LedgerEntry> findByReversalOfIn(Collection<UUID> entryIds);

    /** Hace que dos registros de la misma referencia se procesen en orden. */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(hashtext(:key))) as held", nativeQuery = true)
    int lockReference(@Param("key") String key);

    /** Una referencia sirve una vez, salvo que el pago anterior se haya reversado. */
    @Query("""
            select count(e) > 0 from LedgerEntry e
            where e.kind = com.barboya.finance.LedgerEntry.Kind.PAYMENT and e.method = :method
              and lower(e.reference) = lower(:reference)
              and not exists (select 1 from LedgerEntry r where r.reversalOf = e.id)""")
    boolean referenceInUse(@Param("method") LedgerEntry.Method method, @Param("reference") String reference);
}
