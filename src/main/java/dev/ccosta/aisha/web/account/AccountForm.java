package dev.ccosta.aisha.web.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AccountForm {

    @NotBlank(message = "{accountForm.title.notBlank}")
    @Size(max = 120, message = "{accountForm.title.size}")
    private String title;

    @Size(max = 300, message = "{accountForm.description.size}")
    private String description;

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
}
