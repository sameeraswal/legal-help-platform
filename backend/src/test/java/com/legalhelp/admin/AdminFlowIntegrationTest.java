package com.legalhelp.admin;

import com.legalhelp.auth.dto.AdminUserResponse;
import com.legalhelp.auth.dto.AuthResponse;
import com.legalhelp.auth.dto.LoginRequest;
import com.legalhelp.auth.dto.RegisterRequest;
import com.legalhelp.auth.entity.User;
import com.legalhelp.auth.repository.UserRepository;
import com.legalhelp.common.security.Role;
import com.legalhelp.petition.dto.CaseCategoryResponse;
import com.legalhelp.petition.dto.CaseCategoryUpsertRequest;
import com.legalhelp.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AdminFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private HttpHeaders adminHeaders() {
        User admin = new User(Role.ADMIN, "Platform Admin", "admin-flow@example.com", null, passwordEncoder.encode("AdminPass123!"));
        userRepository.save(admin);
        ResponseEntity<AuthResponse> login = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("admin-flow@example.com", "AdminPass123!"), AuthResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.getBody().accessToken());
        return headers;
    }

    @Test
    void adminCanApproveALawyerAndItIsAudited() {
        HttpHeaders admin = adminHeaders();

        RegisterRequest lawyerRegister = new RegisterRequest(Role.LAWYER, "New Lawyer", "new-lawyer@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> lawyerResponse = restTemplate.postForEntity("/api/auth/register", lawyerRegister, AuthResponse.class);
        Long lawyerId = lawyerResponse.getBody().user().id();
        assertThat(lawyerResponse.getBody().user().role()).isEqualTo(Role.LAWYER);

        ResponseEntity<AdminUserResponse> approve = restTemplate.exchange(
                "/api/admin/users/" + lawyerId + "/approve-lawyer", HttpMethod.POST, new HttpEntity<>(admin), AdminUserResponse.class);
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approve.getBody().lawyerApproved()).isTrue();

        ResponseEntity<String> auditLogs = restTemplate.exchange(
                "/api/admin/audit-logs?entity=user", HttpMethod.GET, new HttpEntity<>(admin), String.class);
        assertThat(auditLogs.getBody()).contains("APPROVE_LAWYER");
    }

    @Test
    void adminCanCreateACategory_visibleToCustomers() {
        HttpHeaders admin = adminHeaders();

        CaseCategoryUpsertRequest request = new CaseCategoryUpsertRequest("test-category", "Test Category", "desc", "consumer-complaint", true);
        ResponseEntity<CaseCategoryResponse> created = restTemplate.exchange(
                "/api/admin/categories", HttpMethod.POST, new HttpEntity<>(request, admin), CaseCategoryResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<CaseCategoryResponse[]> publicList = restTemplate.getForEntity("/api/categories", CaseCategoryResponse[].class);
        assertThat(publicList.getBody()).extracting(CaseCategoryResponse::slug).contains("test-category");
    }

    @Test
    void nonAdminCannotApproveLawyers() {
        RegisterRequest customerRegister = new RegisterRequest(Role.CUSTOMER, "Regular Customer", "regular-customer@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> customerResponse = restTemplate.postForEntity("/api/auth/register", customerRegister, AuthResponse.class);
        HttpHeaders customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(customerResponse.getBody().accessToken());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/users/1/approve-lawyer", HttpMethod.POST, new HttpEntity<>(customerHeaders), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
