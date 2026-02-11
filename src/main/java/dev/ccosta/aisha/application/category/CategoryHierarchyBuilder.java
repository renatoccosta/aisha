package dev.ccosta.aisha.application.category;

import dev.ccosta.aisha.domain.category.Category;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CategoryHierarchyBuilder {

    private CategoryHierarchyBuilder() {
    }

    static List<CategoryOption> buildOptions(List<Category> categories) {
        Map<Long, List<Category>> childrenByParent = new HashMap<>();
        List<Category> roots = new ArrayList<>();

        for (Category category : categories) {
            Category parent = category.getParent();
            if (parent == null || parent.getId() == null) {
                roots.add(category);
                continue;
            }

            childrenByParent.computeIfAbsent(parent.getId(), key -> new ArrayList<>()).add(category);
        }

        List<CategoryOption> options = new ArrayList<>();
        for (Category root : roots) {
            addOptionRecursive(root, 0, childrenByParent, options);
        }
        return options;
    }

    private static void addOptionRecursive(
        Category category,
        int depth,
        Map<Long, List<Category>> childrenByParent,
        List<CategoryOption> options
    ) {
        options.add(new CategoryOption(category.getId(), prefix(depth) + category.getTitle()));
        List<Category> children = childrenByParent.get(category.getId());
        if (children == null) {
            return;
        }

        for (Category child : children) {
            addOptionRecursive(child, depth + 1, childrenByParent, options);
        }
    }

    private static String prefix(int depth) {
        return "  ".repeat(Math.max(0, depth));
    }
}
