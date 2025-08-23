package com.cafm.cafmbackend.integration.controllers;

import com.cafm.cafmbackend.dto.company.*;
import com.cafm.cafmbackend.service.CompanyService;
import com.cafm.cafmbackend.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for CompanyController.
 * Tests multi-tenant company management and tenant isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("CompanyController Integration Tests")
class CompanyControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cafm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private CurrentUserService currentUserService;

    private static final String BASE_URL = "/api/v1/companies";
    private static final UUID TEST_COMPANY_ID = UUID.randomUUID();
    private static final UUID OTHER_COMPANY_ID = UUID.randomUUID();

    // ========== GET All Companies Tests (Super Admin Only) ==========

    @Test
    @Order(1)
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Should get all companies as super admin")
    void testGetAllCompanies_Success() throws Exception {
        // Arrange
        List<CompanyListResponse> companyList = Arrays.asList(
                createCompanyListResponse("Company A", "companya.com"),
                createCompanyListResponse("Company B", "companyb.com")
        );
        Page<CompanyListResponse> page = new PageImpl<>(companyList, PageRequest.of(0, 20), 2);

        when(companyService.getAllCompanies(any(Pageable.class), any(), any(), any(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Company A"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should deny regular admin access to all companies")
    void testGetAllCompanies_AccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Should filter companies by status")
    void testGetAllCompanies_FilterByStatus() throws Exception {
        // Arrange
        List<CompanyListResponse> activeCompanies = Arrays.asList(
                createCompanyListResponse("Active Company", "active.com")
        );
        Page<CompanyListResponse> page = new PageImpl<>(activeCompanies);

        when(companyService.getAllCompanies(any(Pageable.class), eq("ACTIVE"), any(), any(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Should search companies by name")
    void testGetAllCompanies_SearchByName() throws Exception {
        // Arrange
        List<CompanyListResponse> searchResults = Arrays.asList(
                createCompanyListResponse("Tech Corp", "techcorp.com")
        );
        Page<CompanyListResponse> page = new PageImpl<>(searchResults);

        when(companyService.getAllCompanies(any(Pageable.class), any(), any(), any(), eq("Tech")))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("search", "Tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Tech Corp"));
    }

    // ========== GET Company by ID Tests ==========

    @Test
    @Order(5)
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Should get company by ID as super admin")
    void testGetCompanyById_AsSuperAdmin() throws Exception {
        // Arrange
        CompanyResponse company = createCompanyResponse("Test Company", "test.com");
        when(companyService.getCompanyByIdAsDto(TEST_COMPANY_ID)).thenReturn(company);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + TEST_COMPANY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_COMPANY_ID.toString()))
                .andExpect(jsonPath("$.name").value("Test Company"));
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("Should get company by ID as admin from same company")
    void testGetCompanyById_AsAdmin_SameCompany() throws Exception {
        // Arrange
        CompanyResponse company = createCompanyResponse("Test Company", "test.com");
        when(companyService.belongsToCompany(eq(TEST_COMPANY_ID), eq("admin@test.com"))).thenReturn(true);
        when(companyService.getCompanyByIdAsDto(TEST_COMPANY_ID)).thenReturn(company);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + TEST_COMPANY_ID))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    @WithMockUser(roles = "ADMIN", username = "admin@other.com")
    @DisplayName("Should deny admin access to different company")
    void testGetCompanyById_AsAdmin_DifferentCompany() throws Exception {
        // Arrange
        when(companyService.belongsToCompany(eq(OTHER_COMPANY_ID), eq("admin@other.com"))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + OTHER_COMPANY_ID))
                .andExpect(status().isForbidden());
    }

    // ========== CREATE Company Tests (Super Admin Only) ==========

    @Test
    @Order(8)
    @WithMockUser(roles = "SUPER_ADMIN", username = "superadmin@system.com")
    @DisplayName("Should create new company as super admin")
    void testCreateCompany_Success() throws Exception {
        // Arrange
        CompanyCreateRequest request = new CompanyCreateRequest(
                "New Company",
                "new-company",
                "newcompany.com",
                "admin@newcompany.com",
                "Admin",
                "User",
                "+1234567890",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA",
                "PREMIUM",
                100,
                null
        );

        CompanyResponse createdCompany = createCompanyResponse("New Company", "newcompany.com");
        when(companyService.createCompanyFromDto(any(CompanyCreateRequest.class))).thenReturn(createdCompany);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Company"));
    }

    @Test
    @Order(9)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should deny regular admin from creating company")
    void testCreateCompany_AccessDenied() throws Exception {
        // Arrange
        CompanyCreateRequest request = new CompanyCreateRequest(
                "New Company",
                "new-company",
                "newcompany.com",
                "admin@newcompany.com",
                "Admin",
                "User",
                "+1234567890",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA",
                "PREMIUM",
                100,
                null
        );

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Should fail to create company with duplicate domain")
    void testCreateCompany_DuplicateDomain() throws Exception {
        // Arrange
        CompanyCreateRequest request = new CompanyCreateRequest(
                "Duplicate Company",
                "existing",
                "existing.com",
                "admin@existing.com",
                "Admin",
                "User",
                "+1234567890",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA",
                "BASIC",
                10,
                null
        );

        when(companyService.createCompanyFromDto(any(CompanyCreateRequest.class)))
                .thenThrow(new RuntimeException("Domain already exists"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== UPDATE Company Tests ==========

    @Test
    @Order(11)
    @WithMockUser(roles = "SUPER_ADMIN", username = "superadmin@system.com")
    @DisplayName("Should update company as super admin")
    void testUpdateCompany_AsSuperAdmin() throws Exception {
        // Arrange
        CompanyUpdateRequest request = new CompanyUpdateRequest(
                "Updated Company",
                "updated@company.com",
                "+9876543210",
                "456 Updated St",
                "Los Angeles",
                "CA",
                "90001",
                "USA",
                null,
                null,
                null
        );

        CompanyResponse updatedCompany = createCompanyResponse("Updated Company", "test.com");
        when(companyService.updateCompanyFromDto(eq(TEST_COMPANY_ID), any(CompanyUpdateRequest.class)))
                .thenReturn(updatedCompany);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + TEST_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Company"));
    }

    @Test
    @Order(12)
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("Should update company as admin from same company")
    void testUpdateCompany_AsAdmin_SameCompany() throws Exception {
        // Arrange
        CompanyUpdateRequest request = new CompanyUpdateRequest(
                "Updated Company",
                "updated@company.com",
                "+9876543210",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(companyService.belongsToCompany(eq(TEST_COMPANY_ID), eq("admin@test.com"))).thenReturn(true);
        CompanyResponse updatedCompany = createCompanyResponse("Updated Company", "test.com");
        when(companyService.updateCompanyFromDto(eq(TEST_COMPANY_ID), any(CompanyUpdateRequest.class)))
                .thenReturn(updatedCompany);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + TEST_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(13)
    @WithMockUser(roles = "ADMIN", username = "admin@other.com")
    @DisplayName("Should deny admin from updating different company")
    void testUpdateCompany_AsAdmin_DifferentCompany() throws Exception {
        // Arrange
        CompanyUpdateRequest request = new CompanyUpdateRequest(
                "Hacked Company",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(companyService.belongsToCompany(eq(OTHER_COMPANY_ID), eq("admin@other.com"))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + OTHER_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== Multi-Tenant Isolation Tests ==========

    @Test
    @Order(14)
    @WithMockUser(roles = "SUPERVISOR", username = "supervisor@companya.com")
    @DisplayName("Should enforce tenant isolation for supervisor")
    void testTenantIsolation_Supervisor() throws Exception {
        // Test that supervisor can only access their own company
        UUID companyAId = UUID.randomUUID();
        UUID companyBId = UUID.randomUUID();

        // Can access own company
        when(companyService.belongsToCompany(eq(companyAId), eq("supervisor@companya.com"))).thenReturn(true);
        when(companyService.getCompanyByIdAsDto(companyAId))
                .thenReturn(createCompanyResponse("Company A", "companya.com"));

        mockMvc.perform(get(BASE_URL + "/" + companyAId))
                .andExpect(status().isOk());

        // Cannot access other company
        when(companyService.belongsToCompany(eq(companyBId), eq("supervisor@companya.com"))).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/" + companyBId))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(15)
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("Should deny technician access to company management")
    void testTenantIsolation_Technician() throws Exception {
        // Technicians should not have access to company management endpoints
        mockMvc.perform(get(BASE_URL + "/" + TEST_COMPANY_ID))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    // ========== Validation Tests ==========

    @Test
    @Order(16)
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Should validate required fields in create request")
    void testCreateCompany_ValidationError() throws Exception {
        // Arrange - missing required fields
        String invalidRequest = """
                {
                    "name": "Test Company"
                }
                """;

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(17)
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Should validate email format")
    void testCreateCompany_InvalidEmail() throws Exception {
        // Arrange
        String invalidEmailRequest = """
                {
                    "name": "Test Company",
                    "subdomain": "test",
                    "domain": "test.com",
                    "adminEmail": "invalid-email",
                    "adminFirstName": "Admin",
                    "adminLastName": "User",
                    "phone": "+1234567890",
                    "address": "123 Main St",
                    "city": "New York",
                    "state": "NY",
                    "zipCode": "10001",
                    "country": "USA",
                    "subscriptionPlan": "BASIC",
                    "maxUsers": 10
                }
                """;

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmailRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(18)
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Should validate subdomain format")
    void testCreateCompany_InvalidSubdomain() throws Exception {
        // Arrange
        String invalidSubdomainRequest = """
                {
                    "name": "Test Company",
                    "subdomain": "invalid subdomain!",
                    "domain": "test.com",
                    "adminEmail": "admin@test.com",
                    "adminFirstName": "Admin",
                    "adminLastName": "User",
                    "phone": "+1234567890",
                    "address": "123 Main St",
                    "city": "New York",
                    "state": "NY",
                    "zipCode": "10001",
                    "country": "USA",
                    "subscriptionPlan": "BASIC",
                    "maxUsers": 10
                }
                """;

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidSubdomainRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== Helper Methods ==========

    private CompanyListResponse createCompanyListResponse(String name, String domain) {
        return new CompanyListResponse(
                TEST_COMPANY_ID,
                name,
                domain,
                "ACTIVE",
                "PREMIUM",
                100,
                45,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private CompanyResponse createCompanyResponse(String name, String domain) {
        return new CompanyResponse(
                TEST_COMPANY_ID,
                name,
                "test-subdomain",
                domain,
                "admin@" + domain,
                "+1234567890",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA",
                "ACTIVE",
                "PREMIUM",
                100,
                45,
                Map.of("feature1", true, "feature2", false),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}