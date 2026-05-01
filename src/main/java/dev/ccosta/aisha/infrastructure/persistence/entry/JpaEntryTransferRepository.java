package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.transfer.EntryTransfer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaEntryTransferRepository extends JpaRepository<EntryTransfer, Long> {

    @EntityGraph(attributePaths = {"originEntry", "originEntry.account", "destinationEntry", "destinationEntry.account"})
    @Query(
        """
        select et
        from EntryTransfer et
        where et.originEntry.id = :entryId
           or et.destinationEntry.id = :entryId
        """
    )
    Optional<EntryTransfer> findByEntryId(@Param("entryId") Long entryId);

    @Query(
        """
        select (count(et) > 0)
        from EntryTransfer et
        where et.originEntry.id = :entryId
           or et.destinationEntry.id = :entryId
        """
    )
    boolean existsByEntryId(@Param("entryId") Long entryId);

    @EntityGraph(attributePaths = {"originEntry", "destinationEntry"})
    @Query(
        """
        select distinct et
        from EntryTransfer et
        where et.originEntry.id in :entryIds
           or et.destinationEntry.id in :entryIds
        """
    )
    List<EntryTransfer> findAllByEntryIds(@Param("entryIds") Collection<Long> entryIds);

    @Modifying
    @Query(
        """
        delete from EntryTransfer et
        where et.originEntry.id in :entryIds
           or et.destinationEntry.id in :entryIds
        """
    )
    void deleteAllByEntryIds(@Param("entryIds") Collection<Long> entryIds);
}
