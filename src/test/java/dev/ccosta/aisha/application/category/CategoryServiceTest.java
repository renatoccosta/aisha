package dev.ccosta.aisha.application.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.category.CategoryRepository;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategoryWithoutParentWhenNoParentId() {
        Category input = newCategory("Lazer");

        when(categoryRepository.save(input)).thenReturn(input);

        Category created = categoryService.create(input, null);

        assertThat(created.getParent()).isNull();
        verify(categoryRepository).save(input);
    }

    @Test
    void shouldUpdateCategoryParent() {
        Category existing = newCategory(10L, "Alimentação");
        Category parent = newCategory(11L, "Despesas");
        Category updatedData = newCategory("Alimentação e bebidas");

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(existing)).thenReturn(existing);

        Category updated = categoryService.update(10L, updatedData, 11L);

        assertThat(updated.getTitle()).isEqualTo("Alimentação e bebidas");
        assertThat(updated.getParent()).isEqualTo(parent);
    }

    @Test
    void shouldRejectCyclicHierarchyOnUpdate() {
        Category existing = newCategory(10L, "Raiz");
        Category parent = newCategory(11L, "Filha");
        parent.setParent(existing);
        Category updatedData = newCategory("Raiz atualizada");

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> categoryService.update(10L, updatedData, 11L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cycles");
    }

    @Test
    void shouldFindExistingCategoryByTitleIgnoringCase() {
        Category existing = newCategory("Transporte");
        when(categoryRepository.findByTitleIgnoreCase("transporte")).thenReturn(Optional.of(existing));

        Category found = categoryService.findOrCreateByTitle("transporte");

        assertThat(found).isEqualTo(existing);
        verify(categoryRepository, never()).save(existing);
    }

    @Test
    void shouldPreventDeleteWhenCategoryHasEntries() {
        Category existing = newCategory("Saúde");
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByParentId(12L)).thenReturn(false);
        when(entryRepository.existsByCategoryId(12L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteById(12L))
            .isInstanceOf(CategoryInUseException.class)
            .hasMessageContaining("12");

        verify(categoryRepository, never()).deleteById(12L);
    }

    @Test
    void shouldRemoveDuplicateIdsInBulkDelete() {
        Category existing = newCategory("Teste");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(categoryRepository.existsByParentId(2L)).thenReturn(false);
        when(categoryRepository.existsByParentId(3L)).thenReturn(false);
        when(entryRepository.existsByCategoryId(1L)).thenReturn(false);
        when(entryRepository.existsByCategoryId(2L)).thenReturn(false);
        when(entryRepository.existsByCategoryId(3L)).thenReturn(false);

        categoryService.bulkDelete(List.of(1L, 2L, 1L, 3L));

        ArgumentCaptor<java.util.Collection<Long>> idsCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(categoryRepository).deleteByIds(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(1L, 2L, 3L);
    }

    private Category newCategory(String title) {
        Category category = new Category();
        category.setTitle(title);
        return category;
    }

    private Category newCategory(Long id, String title) {
        Category category = new Category();
        category.setTitle(title);
        setId(category, id);
        return category;
    }

    private void setId(Category category, Long id) {
        try {
            java.lang.reflect.Field idField = Category.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
