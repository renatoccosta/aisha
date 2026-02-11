package dev.ccosta.aisha.web.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank(message = "{categoryForm.title.notBlank}")
    @Size(max = 120, message = "{categoryForm.title.size}")
    private String title;

    @Size(max = 300, message = "{categoryForm.description.size}")
    private String description;

    private Long parentId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
