package dev.ccosta.aisha.infrastructure.persistence.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "federated_user_identities")
public class FederatedUserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "local_user_id", nullable = false)
    private LocalUserAccount localUserAccount;

    @Column(name = "provider", nullable = false, length = 40)
    private String provider;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FederatedUserIdentity() {
    }

    public FederatedUserIdentity(
        LocalUserAccount localUserAccount,
        String provider,
        String subject,
        String email,
        Instant createdAt
    ) {
        this.localUserAccount = localUserAccount;
        this.provider = provider;
        this.subject = subject;
        this.email = email;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public LocalUserAccount getLocalUserAccount() {
        return localUserAccount;
    }

    public String getProvider() {
        return provider;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
