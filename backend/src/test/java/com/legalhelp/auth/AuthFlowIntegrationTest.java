package com.legalhelp.auth;

import com.legalhelp.auth.dto.AuthResponse;
import com.legalhelp.auth.dto.LoginRequest;
import com.legalhelp.auth.dto.RegisterRequest;
import com.legalhelp.common.security.Role;
import com.legalhelp.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerThenLogin_returnsMatchingProfile() {
        RegisterRequest register = new RegisterRequest(Role.CUSTOMER, "Asha Verma", "asha@example.com", "9990001111", "Password123!");
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().user().email()).isEqualTo("asha@example.com");
        assertThat(registerResponse.getBody().accessToken()).isNotBlank();

        LoginRequest login = new LoginRequest("asha@example.com", "Password123!");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity("/api/auth/login", login, AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().user().role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void register_rejectsAdminSelfRegistration() {
        RegisterRequest request = new RegisterRequest(Role.ADMIN, "Would-be Admin", "wannabe-admin@example.com", null, "Password123!");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/register", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_rejectsWrongPassword() {
        RegisterRequest register = new RegisterRequest(Role.CUSTOMER, "Ravi Kumar", "ravi@example.com", null, "Password123!");
        restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);

        LoginRequest badLogin = new LoginRequest("ravi@example.com", "wrong-password");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/login", badLogin, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void customerToken_isRejectedByAdminOnlyEndpoint() {
        RegisterRequest register = new RegisterRequest(Role.CUSTOMER, "Neha Singh", "neha@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);
        String accessToken = registerResponse.getBody().accessToken();

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/audit-logs", org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
