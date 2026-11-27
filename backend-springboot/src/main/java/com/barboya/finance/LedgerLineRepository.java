package com.barboya.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LedgerLineRepository extends JpaRepository<LedgerLine, UUID> {

    List<LedgerLine> findByEntryId(UUID entryId);

    @Query("""
            select coalesce(sum(l.debitCop - l.creditCop), 0) from LedgerLine l
            where l.account = com.barboya.finance.LedgerLine.Account.RECEIVABLE and l.partyId = :partyId""")
    long balanceOf(@Param("partyId") UUID partyId);

    @Query("""
            select l.partyId from LedgerLine l
            where l.account = com.barboya.finance.LedgerLine.Account.RECEIVABLE and l.partyId in :parties
            group by l.partyId having sum(l.debitCop - l.creditCop) > :limit""")
    List<UUID> over(@Param("parties") Collection<UUID> parties, @Param("limit") long limit);

    @Query("""
            select coalesce(sum(l.debitCop - l.creditCop), 0) from LedgerLine l where l.account = :account""")
    long net(@Param("account") LedgerLine.Account account);

    interface Debtor {
        UUID getPartyId();

        String getName();

        String getRole();

        long getBalanceCop();
    }

    @Query(value = """
            select u.id as partyId, u.full_name as name, u.role as role,
                   sum(l.debit_cop - l.credit_cop) as balanceCop
            from ledger_lines l join users u on u.id = l.party_id
            where l.account = 'RECEIVABLE'
            group by u.id, u.full_name, u.role
            having sum(l.debit_cop - l.credit_cop) > 0
            order by balanceCop desc
            limit 100""", nativeQuery = true)
    List<Debtor> debtors();

    interface Activity {
        long getOrders();

        long getSalesCop();

        long getCommissionsCop();
    }

    /** Pedidos entregados desde una fecha. Consulta directa para no depender del modulo de pedidos. */
    @Query(value = """
            select count(*) as orders,
                   coalesce(sum(total_cop), 0) as salesCop,
                   coalesce(sum(merchant_commission_cop + courier_commission_cop), 0) as commissionsCop
            from orders where status = 'DELIVERED' and delivered_at >= :since""", nativeQuery = true)
    Activity activitySince(@Param("since") Instant since);
}
