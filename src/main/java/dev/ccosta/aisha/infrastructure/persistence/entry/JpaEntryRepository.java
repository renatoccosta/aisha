package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.Entry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findTop100ByOrderBySettlementDateDescIdDesc();
}
