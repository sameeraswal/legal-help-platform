package com.legalhelp.auth.entity;

import com.legalhelp.common.security.Role;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    // Lawyer-only onboarding fields; null for CUSTOMER/ADMIN.
    @Column(name = "kyc_document_url")
    private String kycDocumentUrl;

    @Column(name = "bank_account_details")
    private String bankAccountDetails;

    @Column(name = "lawyer_approved")
    private Boolean lawyerApproved;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {
    }

    public User(Role role, String name, String email, String phone, String passwordHash) {
        this.role = role;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        if (role == Role.LAWYER) {
            this.lawyerApproved = false;
        }
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getKycDocumentUrl() {
        return kycDocumentUrl;
    }

    public void setKycDocumentUrl(String kycDocumentUrl) {
        this.kycDocumentUrl = kycDocumentUrl;
    }

    public String getBankAccountDetails() {
        return bankAccountDetails;
    }

    public void setBankAccountDetails(String bankAccountDetails) {
        this.bankAccountDetails = bankAccountDetails;
    }

    public Boolean getLawyerApproved() {
        return lawyerApproved;
    }

    public void setLawyerApproved(Boolean lawyerApproved) {
        this.lawyerApproved = lawyerApproved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
