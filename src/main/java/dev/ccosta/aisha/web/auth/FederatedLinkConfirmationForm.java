package dev.ccosta.aisha.web.auth;

import jakarta.validation.constraints.NotBlank;

public class FederatedLinkConfirmationForm {

    @NotBlank
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
