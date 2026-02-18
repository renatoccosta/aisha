package dev.ccosta.aisha.web.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public class LocalUserAccountForm {

    @NotBlank(message = "{localUserForm.username.notBlank}")
    @Size(max = 80, message = "{localUserForm.username.size}")
    private String username;

    @Size(max = 120, message = "{localUserForm.password.size}")
    private String password;

    @Size(max = 120, message = "{localUserForm.passwordConfirmation.size}")
    private String passwordConfirmation;

    private boolean enabled = true;

    @AssertTrue(message = "{localUserForm.passwordConfirmation.match}")
    public boolean isPasswordConfirmationValid() {
        if (!StringUtils.hasText(password) && !StringUtils.hasText(passwordConfirmation)) {
            return true;
        }
        return password != null && password.equals(passwordConfirmation);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
