package dev.ccosta.aisha.infrastructure.persistence.entry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.shared.PagedResult;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.sql.init.mode=never")
class JpaEntryRepositoryTest {

    @Autowired
    private EntryRepositoryAdapter entryRepositoryAdapter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFilterByDescriptionIgnoringCaseAndAccents() {
        Account account = persistAccount("Conta teste");
        Category category = persistCategory("Categoria teste");
        Entry matchingEntry = persistEntry(account, category, "Café no MERCADO");
        persistEntry(account, category, "Pagamento de aluguel");
        entityManager.flush();
        entityManager.clear();

        PagedResult<Entry> result = entryRepositoryAdapter.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            null,
            null,
            "cafe",
            false,
            false,
            0,
            25
        );

        assertThat(result.items())
            .extracting(Entry::getId)
            .containsExactly(matchingEntry.getId());
    }

    @Test
    void shouldApplyAdvancedDescriptionSearchSyntax() {
        Account account = persistAccount("Conta teste");
        Category category = persistCategory("Categoria teste");
        Entry mercado = persistEntry(account, category, "Mercado bairro");
        Entry farmacia = persistEntry(account, category, "Farmácia centro");
        persistEntry(account, category, "Mercado ifood");
        persistEntry(account, category, "Padaria");
        entityManager.flush();
        entityManager.clear();

        PagedResult<Entry> result = entryRepositoryAdapter.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            account.getId(),
            null,
            "merc* farm?cia -ifood",
            false,
            false,
            0,
            25
        );

        assertThat(result.items())
            .extracting(Entry::getId)
            .containsExactly(farmacia.getId(), mercado.getId());
    }

    @Test
    void shouldApplyQuotedDescriptionSearchPhrase() {
        Account account = persistAccount("Conta teste");
        Category category = persistCategory("Categoria teste");
        Entry matchingEntry = persistEntry(account, category, "Café da manhã");
        persistEntry(account, category, "Café no mercado pela manhã");
        entityManager.flush();
        entityManager.clear();

        PagedResult<Entry> result = entryRepositoryAdapter.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            account.getId(),
            null,
            "\"cafe da manha\"",
            false,
            false,
            0,
            25
        );

        assertThat(result.items())
            .extracting(Entry::getId)
            .containsExactly(matchingEntry.getId());
    }

    @Test
    void shouldListEntriesWithoutDescriptionFilter() {
        Account account = persistAccount("Conta teste");
        Category category = persistCategory("Categoria teste");
        Entry newerEntry = persistEntry(account, category, "Compra mercado");
        Entry olderEntry = persistEntry(account, category, "Padaria");
        olderEntry.setSettlementDate(LocalDate.of(2026, 1, 5));
        entityManager.flush();
        entityManager.clear();

        PagedResult<Entry> result = entryRepositoryAdapter.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            account.getId(),
            null,
            "   ",
            false,
            false,
            0,
            25
        );

        assertThat(result.items())
            .extracting(Entry::getId)
            .containsExactly(newerEntry.getId(), olderEntry.getId());
    }

    private Account persistAccount(String title) {
        Account account = new Account();
        account.setTitle(title);
        account.setInitialBalance(BigDecimal.ZERO);
        account.setInitialBalanceDate(LocalDate.of(2026, 1, 1));
        entityManager.persist(account);
        entityManager.flush();
        return account;
    }

    private Category persistCategory(String title) {
        Category category = new Category();
        category.setTitle(title);
        entityManager.persist(category);
        entityManager.flush();
        return category;
    }

    private Entry persistEntry(Account account, Category category, String description) {
        Entry entry = new Entry();
        entry.setAccount(account);
        entry.setCategory(category);
        entry.setDescription(description);
        entry.setMovementDate(LocalDate.of(2026, 1, 10));
        entry.setSettlementDate(LocalDate.of(2026, 1, 10));
        entry.setAmount(new BigDecimal("10.00"));
        entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.NONE);
        entityManager.persist(entry);
        return entry;
    }
}
