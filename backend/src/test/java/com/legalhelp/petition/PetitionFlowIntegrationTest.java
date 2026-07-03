package com.legalhelp.petition;

import com.legalhelp.auth.dto.AuthResponse;
import com.legalhelp.auth.dto.RegisterRequest;
import com.legalhelp.common.llm.LlmClient;
import com.legalhelp.common.security.Role;
import com.legalhelp.petition.dto.*;
import com.legalhelp.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PetitionFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private LlmClient llmClient;

    private HttpHeaders authHeaders;

    @BeforeEach
    void registerCustomer() {
        when(llmClient.generate(anyString(), any(), anyString()))
                .thenReturn("This is a drafted petition body.\n\nSecond paragraph with more detail.");

        RegisterRequest register = new RegisterRequest(Role.CUSTOMER, "Test Customer", "petition-flow@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(response.getBody().accessToken());
    }

    @Test
    void fullPetitionFlow_intakeToDownload() {
        ResponseEntity<CaseCategoryResponse[]> categories = restTemplate.getForEntity("/api/categories", CaseCategoryResponse[].class);
        assertThat(categories.getBody()).isNotEmpty();
        Long categoryId = categories.getBody()[0].id();

        CaseIntakeRequest intake = new CaseIntakeRequest(categoryId, Map.of("partyName", "Asha Verma", "amount", "5000"));
        ResponseEntity<CaseResponse> caseResponse = restTemplate.exchange("/api/cases", HttpMethod.POST,
                new HttpEntity<>(intake, authHeaders), CaseResponse.class);
        assertThat(caseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long caseId = caseResponse.getBody().id();

        ResponseEntity<CaseResponse> submitResponse = restTemplate.exchange("/api/cases/" + caseId + "/submit", HttpMethod.POST,
                new HttpEntity<>(authHeaders), CaseResponse.class);
        assertThat(submitResponse.getBody().status().name()).isEqualTo("SUBMITTED");

        ResponseEntity<PetitionResponse> petitionResponse = restTemplate.exchange(
                "/api/cases/" + caseId + "/petitions/generate", HttpMethod.POST,
                new HttpEntity<>(authHeaders), PetitionResponse.class);
        assertThat(petitionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(petitionResponse.getBody().version()).isEqualTo(1);
        Long petitionId = petitionResponse.getBody().id();

        ResponseEntity<byte[]> pdfResponse = restTemplate.exchange(
                "/api/cases/" + caseId + "/petitions/" + petitionId + "/pdf", HttpMethod.GET,
                new HttpEntity<>(authHeaders), byte[].class);
        assertThat(pdfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdfResponse.getBody()).isNotEmpty();

        // Regeneration must create a new version, never overwrite.
        ResponseEntity<PetitionResponse> secondGeneration = restTemplate.exchange(
                "/api/cases/" + caseId + "/petitions/generate", HttpMethod.POST,
                new HttpEntity<>(authHeaders), PetitionResponse.class);
        assertThat(secondGeneration.getBody().version()).isEqualTo(2);

        ResponseEntity<PetitionResponse[]> versions = restTemplate.exchange(
                "/api/cases/" + caseId + "/petitions", HttpMethod.GET,
                new HttpEntity<>(authHeaders), PetitionResponse[].class);
        assertThat(versions.getBody()).hasSize(2);
    }

    @Test
    void anotherCustomer_cannotAccessSomeoneElsesCase() {
        CaseIntakeRequest intake = new CaseIntakeRequest(1L, Map.of("partyName", "Owner"));
        ResponseEntity<CaseResponse> caseResponse = restTemplate.exchange("/api/cases", HttpMethod.POST,
                new HttpEntity<>(intake, authHeaders), CaseResponse.class);
        Long caseId = caseResponse.getBody().id();

        RegisterRequest otherRegister = new RegisterRequest(Role.CUSTOMER, "Other Customer", "other-petition-flow@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> otherResponse = restTemplate.postForEntity("/api/auth/register", otherRegister, AuthResponse.class);
        HttpHeaders otherHeaders = new HttpHeaders();
        otherHeaders.setBearerAuth(otherResponse.getBody().accessToken());

        ResponseEntity<String> forbidden = restTemplate.exchange("/api/cases/" + caseId, HttpMethod.GET,
                new HttpEntity<>(otherHeaders), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
