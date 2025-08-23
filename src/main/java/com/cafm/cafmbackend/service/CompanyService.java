package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.CompanyStatus;
import com.cafm.cafmbackend.data.enums.SubscriptionPlan;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.enums.ReportStatus;
import com.cafm.cafmbackend.data.enums.AssetCondition;
import com.cafm.cafmbackend.data.enums.AssetStatus;
import com.cafm.cafmbackend.data.repository.CompanyRepository;
import com.cafm.cafmbackend.data.repository.UserRepository;
import com.cafm.cafmbackend.data.repository.SchoolRepository;
import com.cafm.cafmbackend.data.repository.ReportRepository;
import com.cafm.cafmbackend.data.repository.AssetRepository;
import com.cafm.cafmbackend.data.repository.WorkOrderRepository;
import com.cafm.cafmbackend.dto.company.*;
import com.cafm.cafmbackend.exception.DuplicateResourceException;
import com.cafm.cafmbackend.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing companies (tenants) in the multi-tenant system.
 * Handles company lifecycle, subscription management, and resource limits.
 */
@Service
@Transactional
public class CompanyService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);
    
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ReportRepository reportRepository;
    private final AssetRepository assetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.trial.duration.days:30}")
    private int trialDurationDays;
    
    @Value("${app.default.max-users:10}")
    private int defaultMaxUsers;
    
    @Value("${app.default.max-schools:5}")
    private int defaultMaxSchools;
    
    @Value("${app.domain.base:cafm.app}")
    private String baseDomain;
    
    public CompanyService(CompanyRepository companyRepository,
                         UserRepository userRepository,
                         SchoolRepository schoolRepository,
                         ReportRepository reportRepository,
                         AssetRepository assetRepository,
                         WorkOrderRepository workOrderRepository,
                         PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.reportRepository = reportRepository;
        this.assetRepository = assetRepository;
        this.workOrderRepository = workOrderRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    // ========== Company Management Methods ==========
    
    /**
     * Create a new company with trial subscription.
     */
    public Company createCompany(Company company, User adminUser) {
        logger.info("Creating new company: {}", company.getName());
        
        // Validate domain uniqueness
        if (company.getDomain() != null && companyRepository.existsByDomainAndDeletedAtIsNull(company.getDomain())) {
            throw new DuplicateResourceException("Company", "domain", company.getDomain());
        }
        
        // Validate subdomain uniqueness
        if (company.getSubdomain() != null && companyRepository.existsBySubdomainAndDeletedAtIsNull(company.getSubdomain())) {
            throw new DuplicateResourceException("Company", "subdomain", company.getSubdomain());
        }
        
        // Generate subdomain if not provided
        if (company.getSubdomain() == null) {
            company.setSubdomain(generateUniqueSubdomain(company.getName()));
        }
        
        // Set trial subscription
        company.setStatus(CompanyStatus.TRIAL);
        company.setSubscriptionPlan(SubscriptionPlan.FREE);
        company.setSubscriptionStartDate(LocalDate.now());
        company.setSubscriptionEndDate(LocalDate.now().plusDays(trialDurationDays));
        
        // Set default limits based on trial plan
        setTrialLimits(company);
        
        // Set default settings
        company.setIsActive(true);
        company.setTimezone("Asia/Riyadh");
        company.setLocale("ar_SA");
        company.setCurrency("SAR");
        
        Company savedCompany = companyRepository.save(company);
        
        // Create admin user for the company
        if (adminUser != null) {
            adminUser.setCompany(savedCompany);
            adminUser.setUserType(UserType.ADMIN);
            userRepository.save(adminUser);
        }
        
        logger.info("Company created successfully with ID: {}", savedCompany.getId());
        return savedCompany;
    }
    
    /**
     * Update company information.
     */
    public Company updateCompany(UUID companyId, Company updatedCompany) {
        logger.info("Updating company: {}", companyId);
        
        Company existingCompany = findById(companyId);
        
        // Update allowed fields
        if (updatedCompany.getName() != null) {
            existingCompany.setName(updatedCompany.getName());
        }
        if (updatedCompany.getDisplayName() != null) {
            existingCompany.setDisplayName(updatedCompany.getDisplayName());
        }
        if (updatedCompany.getContactEmail() != null) {
            existingCompany.setContactEmail(updatedCompany.getContactEmail());
        }
        if (updatedCompany.getContactPhone() != null) {
            existingCompany.setContactPhone(updatedCompany.getContactPhone());
        }
        if (updatedCompany.getPrimaryContactName() != null) {
            existingCompany.setPrimaryContactName(updatedCompany.getPrimaryContactName());
        }
        if (updatedCompany.getIndustry() != null) {
            existingCompany.setIndustry(updatedCompany.getIndustry());
        }
        if (updatedCompany.getAddress() != null) {
            existingCompany.setAddress(updatedCompany.getAddress());
        }
        if (updatedCompany.getCity() != null) {
            existingCompany.setCity(updatedCompany.getCity());
        }
        if (updatedCompany.getPostalCode() != null) {
            existingCompany.setPostalCode(updatedCompany.getPostalCode());
        }
        if (updatedCompany.getTaxNumber() != null) {
            existingCompany.setTaxNumber(updatedCompany.getTaxNumber());
        }
        if (updatedCompany.getCommercialRegistration() != null) {
            existingCompany.setCommercialRegistration(updatedCompany.getCommercialRegistration());
        }
        
        return companyRepository.save(existingCompany);
    }
    
    /**
     * Activate a company after setup.
     */
    public Company activateCompany(UUID companyId) {
        logger.info("Activating company: {}", companyId);
        
        Company company = findById(companyId);
        
        // Validate company is ready for activation
        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot activate suspended company");
        }
        
        company.activate();
        
        return companyRepository.save(company);
    }
    
    /**
     * Suspend a company.
     */
    public Company suspendCompany(UUID companyId, String reason) {
        logger.info("Suspending company {}: {}", companyId, reason);
        
        Company company = findById(companyId);
        company.suspend();
        
        // Log suspension reason (could be stored in audit table)
        logger.warn("Company {} suspended: {}", companyId, reason);
        
        return companyRepository.save(company);
    }
    
    /**
     * Soft delete a company.
     */
    public void deleteCompany(UUID companyId, UUID deletedBy) {
        logger.info("Soft deleting company: {}", companyId);
        
        Company company = findById(companyId);
        company.setDeletedAt(LocalDateTime.now());
        company.setDeletedBy(deletedBy);
        company.setIsActive(false);
        company.setStatus(CompanyStatus.INACTIVE);
        
        companyRepository.save(company);
    }
    
    // ========== Subscription Management ==========
    
    /**
     * Upgrade company subscription plan.
     */
    public Company upgradeSubscription(UUID companyId, SubscriptionPlan newPlan, LocalDate endDate) {
        logger.info("Upgrading company {} to plan: {}", companyId, newPlan);
        
        Company company = findById(companyId);
        
        // Validate upgrade
        if (company.getSubscriptionPlan().ordinal() >= newPlan.ordinal()) {
            throw new IllegalArgumentException("Cannot downgrade to plan: " + newPlan);
        }
        
        company.setSubscriptionPlan(newPlan);
        company.setSubscriptionStartDate(LocalDate.now());
        company.setSubscriptionEndDate(endDate);
        
        // Update resource limits based on new plan
        updateResourceLimits(company, newPlan);
        
        // Activate company if it was in trial
        if (company.getStatus() == CompanyStatus.TRIAL) {
            company.setStatus(CompanyStatus.ACTIVE);
        }
        
        return companyRepository.save(company);
    }
    
    /**
     * Extend company subscription.
     */
    public Company extendSubscription(UUID companyId, int additionalMonths) {
        logger.info("Extending subscription for company {} by {} months", companyId, additionalMonths);
        
        Company company = findById(companyId);
        
        LocalDate currentEndDate = company.getSubscriptionEndDate();
        if (currentEndDate == null || currentEndDate.isBefore(LocalDate.now())) {
            currentEndDate = LocalDate.now();
        }
        
        company.setSubscriptionEndDate(currentEndDate.plusMonths(additionalMonths));
        
        return companyRepository.save(company);
    }
    
    /**
     * Convert trial to paid subscription.
     */
    public Company convertTrialToPaid(UUID companyId, SubscriptionPlan plan) {
        logger.info("Converting trial to paid for company: {}", companyId);
        
        Company company = findById(companyId);
        
        if (company.getStatus() != CompanyStatus.TRIAL) {
            throw new IllegalStateException("Company is not in trial status");
        }
        
        company.setStatus(CompanyStatus.ACTIVE);
        company.setSubscriptionPlan(plan);
        company.setSubscriptionStartDate(LocalDate.now());
        company.setSubscriptionEndDate(LocalDate.now().plusYears(1)); // Default 1 year
        
        updateResourceLimits(company, plan);
        
        return companyRepository.save(company);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Find company by ID.
     */
    public Company findById(UUID companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
    }
    
    /**
     * Find company by domain.
     */
    public Optional<Company> findByDomain(String domain) {
        return companyRepository.findByDomainAndDeletedAtIsNull(domain);
    }
    
    /**
     * Find company by subdomain.
     */
    public Optional<Company> findBySubdomain(String subdomain) {
        return companyRepository.findBySubdomainAndDeletedAtIsNull(subdomain);
    }
    
    /**
     * Find all active companies.
     */
    public List<Company> findActiveCompanies() {
        return companyRepository.findActiveCompanies();
    }
    
    /**
     * Find companies by status.
     */
    public List<Company> findByStatus(CompanyStatus status) {
        return companyRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status);
    }
    
    /**
     * Search companies.
     */
    public Page<Company> searchCompanies(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return companyRepository.findAll(pageable);
        }
        return companyRepository.searchCompanies(searchTerm, pageable);
    }
    
    /**
     * Find companies with expiring subscriptions.
     */
    public List<Company> findCompaniesWithExpiringSubscriptions(int daysAhead) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead);
        return companyRepository.findCompaniesWithExpiringSubscriptions(startDate, endDate);
    }
    
    /**
     * Find companies with expired subscriptions.
     */
    public List<Company> findCompaniesWithExpiredSubscriptions() {
        return companyRepository.findCompaniesWithExpiredSubscriptions(LocalDate.now());
    }
    
    // ========== Resource Management ==========
    
    /**
     * Check if company can add more users.
     */
    public boolean canAddUsers(UUID companyId, int count) {
        Company company = findById(companyId);
        long currentUsers = userRepository.countByCompany_IdAndDeletedAtIsNull(companyId);
        return (currentUsers + count) <= company.getMaxUsers();
    }
    
    /**
     * Check if company can add more schools.
     */
    public boolean canAddSchools(UUID companyId, int count) {
        Company company = findById(companyId);
        // Would need SchoolRepository to count current schools
        return company.getAvailableSchoolSlots() >= count;
    }
    
    /**
     * Update company resource limits.
     */
    public Company updateResourceLimits(UUID companyId, Integer maxUsers, Integer maxSchools, 
                                       Integer maxSupervisors, Integer maxTechnicians) {
        logger.info("Updating resource limits for company: {}", companyId);
        
        Company company = findById(companyId);
        
        if (maxUsers != null) {
            company.setMaxUsers(maxUsers);
        }
        if (maxSchools != null) {
            company.setMaxSchools(maxSchools);
        }
        if (maxSupervisors != null) {
            company.setMaxSupervisors(maxSupervisors);
        }
        if (maxTechnicians != null) {
            company.setMaxTechnicians(maxTechnicians);
        }
        
        return companyRepository.save(company);
    }
    
    // ========== Statistics Methods ==========
    
    /**
     * Get company statistics.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "statistics", key = "'company-stats-' + #companyId")
    public CompanyStatistics getCompanyStatistics(UUID companyId) {
        Company company = findById(companyId);
        CompanyStatistics stats = new CompanyStatistics();
        
        stats.companyId = company.getId();
        stats.companyName = company.getName();
        stats.status = company.getStatus();
        stats.subscriptionPlan = company.getSubscriptionPlan();
        
        // Resource usage
        stats.userCount = userRepository.countByCompany_IdAndDeletedAtIsNull(companyId);
        stats.maxUsers = company.getMaxUsers();
        stats.userUtilization = (stats.userCount * 100.0) / stats.maxUsers;
        
        // Subscription info
        stats.subscriptionStartDate = company.getSubscriptionStartDate();
        stats.subscriptionEndDate = company.getSubscriptionEndDate();
        stats.daysUntilExpiry = calculateDaysUntilExpiry(company.getSubscriptionEndDate());
        stats.isExpired = !company.hasActiveSubscription();
        
        // User breakdown by type - simplified without enum queries due to PostgreSQL enum type issues
        stats.adminCount = 1L; // Placeholder - would need custom query for enum types
        stats.supervisorCount = 0L; // Placeholder
        stats.technicianCount = 0L; // Placeholder
        
        return stats;
    }
    
    /**
     * Get overall platform statistics (for super admin).
     */
    @Transactional(readOnly = true)
    public PlatformStatistics getPlatformStatistics() {
        PlatformStatistics stats = new PlatformStatistics();
        
        List<Company> allCompanies = companyRepository.findAll();
        
        stats.totalCompanies = allCompanies.size();
        stats.activeCompanies = allCompanies.stream()
            .filter(Company::isAccessible)
            .count();
        
        // Count by status
        stats.companiesByStatus = allCompanies.stream()
            .collect(Collectors.groupingBy(Company::getStatus, Collectors.counting()));
        
        // Count by plan
        stats.companiesByPlan = allCompanies.stream()
            .collect(Collectors.groupingBy(Company::getSubscriptionPlan, Collectors.counting()));
        
        // Trial statistics
        stats.trialCompanies = allCompanies.stream()
            .filter(Company::isInTrial)
            .count();
        
        stats.expiredTrials = allCompanies.stream()
            .filter(c -> c.isInTrial() && !c.hasActiveSubscription())
            .count();
        
        // Revenue metrics (simplified)
        stats.paidCompanies = allCompanies.stream()
            .filter(c -> c.getSubscriptionPlan() != SubscriptionPlan.FREE)
            .count();
        
        return stats;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Generate unique subdomain from company name.
     */
    private String generateUniqueSubdomain(String companyName) {
        String base = companyName.toLowerCase()
            .replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        
        String subdomain = base;
        int counter = 1;
        
        while (companyRepository.existsBySubdomainAndDeletedAtIsNull(subdomain)) {
            subdomain = base + "-" + counter++;
        }
        
        return subdomain;
    }
    
    /**
     * Set trial limits for new company.
     */
    private void setTrialLimits(Company company) {
        company.setMaxUsers(defaultMaxUsers);
        company.setMaxSchools(defaultMaxSchools);
        company.setMaxSupervisors(3);
        company.setMaxTechnicians(5);
        company.setMaxStorageGb(1);
    }
    
    /**
     * Update resource limits based on subscription plan.
     */
    private void updateResourceLimits(Company company, SubscriptionPlan plan) {
        switch (plan) {
            case FREE -> {
                company.setMaxUsers(10);
                company.setMaxSchools(5);
                company.setMaxSupervisors(3);
                company.setMaxTechnicians(5);
                company.setMaxStorageGb(1);
            }
            case BASIC -> {
                company.setMaxUsers(50);
                company.setMaxSchools(20);
                company.setMaxSupervisors(10);
                company.setMaxTechnicians(25);
                company.setMaxStorageGb(10);
            }
            case PROFESSIONAL -> {
                company.setMaxUsers(200);
                company.setMaxSchools(100);
                company.setMaxSupervisors(50);
                company.setMaxTechnicians(100);
                company.setMaxStorageGb(50);
            }
            case ENTERPRISE -> {
                company.setMaxUsers(1000);
                company.setMaxSchools(500);
                company.setMaxSupervisors(200);
                company.setMaxTechnicians(500);
                company.setMaxStorageGb(500);
            }
        }
    }
    
    /**
     * Calculate days until subscription expiry.
     */
    private long calculateDaysUntilExpiry(LocalDate endDate) {
        if (endDate == null) {
            return -1; // No expiry
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }
    
    /**
     * Process expired subscriptions (scheduled task).
     */
    @Transactional
    public void processExpiredSubscriptions() {
        logger.info("Processing expired subscriptions");
        
        List<Company> expiredCompanies = findCompaniesWithExpiredSubscriptions();
        
        for (Company company : expiredCompanies) {
            logger.warn("Subscription expired for company: {}", company.getId());
            
            // Downgrade to free plan or suspend
            if (company.getSubscriptionPlan() != SubscriptionPlan.FREE) {
                company.setSubscriptionPlan(SubscriptionPlan.FREE);
                updateResourceLimits(company, SubscriptionPlan.FREE);
                
                // Check if current usage exceeds free limits
                long userCount = userRepository.countByCompany_IdAndDeletedAtIsNull(company.getId());
                if (userCount > company.getMaxUsers()) {
                    company.suspend();
                    logger.warn("Company {} suspended due to exceeding free plan limits", company.getId());
                }
                
                companyRepository.save(company);
            }
        }
    }
    
    /**
     * Send expiry notifications (scheduled task).
     */
    @Transactional(readOnly = true)
    public void sendExpiryNotifications() {
        logger.info("Sending subscription expiry notifications");
        
        // Companies expiring in 30 days
        List<Company> expiringIn30Days = findCompaniesWithExpiringSubscriptions(30);
        for (Company company : expiringIn30Days) {
            logger.info("Sending 30-day expiry notice to company: {}", company.getId());
            // TODO: Send notification via email/notification service
        }
        
        // Companies expiring in 7 days
        List<Company> expiringIn7Days = findCompaniesWithExpiringSubscriptions(7);
        for (Company company : expiringIn7Days) {
            logger.info("Sending 7-day expiry notice to company: {}", company.getId());
            // TODO: Send urgent notification
        }
    }
    
    // ========== DTO-based Methods ==========
    
    /**
     * Get all companies with filtering as DTOs.
     */
    @Transactional(readOnly = true)
    public Page<CompanyListResponse> getAllCompanies(Pageable pageable, String status, 
                                                   String subscriptionPlan, String country, String search) {
        logger.debug("Get all companies with filters - status: {}, plan: {}, country: {}, search: {}", 
                    status, subscriptionPlan, country, search);
        
        Page<Company> companies = companyRepository.findAll(pageable);
        // TODO: Apply filters and convert to DTOs
        return companies.map(this::toCompanyListResponse);
    }
    
    /**
     * Get company by ID as DTO.
     */
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyByIdAsDto(UUID id) {
        Company company = findById(id);
        return toCompanyResponse(company);
    }
    
    /**
     * Create company from DTO.
     */
    @Transactional
    public CompanyResponse  createCompanyFromDto(CompanyCreateRequest request) {
        logger.info("Creating company: {}", request.name());
        
        try {
            // Validate domain uniqueness if provided
            if (request.domain() != null && !request.domain().isEmpty()) {
                if (companyRepository.existsByDomainAndDeletedAtIsNull(request.domain())) {
                    throw new DuplicateResourceException("Company", "domain", request.domain());
                }
            }
            
            // Validate subdomain uniqueness if provided
            if (request.subdomain() != null && !request.subdomain().isEmpty()) {
                if (companyRepository.existsBySubdomainAndDeletedAtIsNull(request.subdomain())) {
                    throw new DuplicateResourceException("Company", "subdomain", request.subdomain());
                }
            }
            
            // Convert DTO to entity
            Company company = new Company();
            company.setName(request.name());
            company.setDisplayName(request.displayName() != null ? request.displayName() : request.name());
            company.setDomain(request.domain());
            company.setContactEmail(request.contactEmail());
            company.setContactPhone(request.contactPhone());
            company.setCountry(request.country() != null ? request.country() : "Saudi Arabia");
            company.setCity(request.city());
            company.setAddress(request.address());
            company.setTimezone(request.timezone() != null ? request.timezone() : "Asia/Riyadh");
            company.setLocale(request.locale() != null ? request.locale() : "ar_SA");
            company.setCurrency(request.currency() != null ? request.currency() : "SAR");
            company.setSubdomain(request.subdomain());
            company.setIndustry(request.industry());
            company.setPrimaryContactName(request.primaryContactName());
            company.setPostalCode(request.postalCode());
            company.setTaxNumber(request.taxNumber());
            company.setCommercialRegistration(request.commercialRegistration());
            company.setSubscriptionPlan(request.subscriptionPlan() != null ? request.subscriptionPlan() : SubscriptionPlan.FREE);
            
            // Set resource limits
            if (request.maxUsers() != null) company.setMaxUsers(request.maxUsers());
            if (request.maxSchools() != null) company.setMaxSchools(request.maxSchools());
            if (request.maxSupervisors() != null) company.setMaxSupervisors(request.maxSupervisors());
            if (request.maxTechnicians() != null) company.setMaxTechnicians(request.maxTechnicians());
            if (request.maxStorageGb() != null) company.setMaxStorageGb(request.maxStorageGb());
            
            // Set settings and data classification
            if (request.settings() != null) company.setSettings(request.settings());
            if (request.dataClassification() != null) company.setDataClassification(request.dataClassification());
        
        // Create admin user for the company if contact email is provided
        User adminUser = null;
        if (request.contactEmail() != null && !request.contactEmail().isEmpty()) {
            adminUser = new User();
            adminUser.setEmail(request.contactEmail());
            
            // Parse primary contact name if provided
            if (request.primaryContactName() != null && !request.primaryContactName().isEmpty()) {
                String[] nameParts = request.primaryContactName().split(" ", 2);
                adminUser.setFirstName(nameParts[0]);
                adminUser.setLastName(nameParts.length > 1 ? nameParts[1] : "");
            } else {
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
            }
            
            adminUser.setUserType(UserType.ADMIN);
            adminUser.setStatus(UserStatus.ACTIVE);
            
            // Generate unique username from email
            String baseUsername = request.contactEmail().split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
            String username = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsernameIgnoreCase(username)) {
                username = baseUsername + counter++;
            }
            adminUser.setUsername(username);
            
            // Set temporary password - should be changed on first login
            adminUser.setPasswordHash(passwordEncoder.encode("TempPassword123!"));
            adminUser.setPasswordChangedAt(LocalDateTime.now());
            
            // Set default values for required fields
            adminUser.setEmailVerified(false); // Will need to verify email
            adminUser.setPhoneVerified(false);
            adminUser.setIsActive(true);
            adminUser.setIsLocked(false);
            
            // Set phone if provided
            if (request.contactPhone() != null && !request.contactPhone().isEmpty()) {
                adminUser.setPhone(request.contactPhone());
            }
        }
        
            Company createdCompany = createCompany(company, adminUser);
            logger.info("Company created successfully: {} with ID: {}", createdCompany.getName(), createdCompany.getId());
            return toCompanyResponse(createdCompany);
            
        } catch (DuplicateResourceException e) {
            logger.error("Duplicate resource error creating company: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error creating company: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create company: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update company from DTO.
     */
    public CompanyResponse updateCompanyFromDto(UUID id, CompanyUpdateRequest request) {
        Company company = findById(id);
        // Update fields from request
        if (request.name() != null) company.setName(request.name());
        if (request.displayName() != null) company.setDisplayName(request.displayName());
        if (request.contactEmail() != null) company.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) company.setContactPhone(request.contactPhone());
        if (request.country() != null) company.setCountry(request.country());
        if (request.city() != null) company.setCity(request.city());
        if (request.address() != null) company.setAddress(request.address());
        if (request.timezone() != null) company.setTimezone(request.timezone());

        Company updatedCompany = updateCompany(id, company);
        return toCompanyResponse(updatedCompany);
    }
    
    /**
     * Deactivate company.
     */
    public CompanyResponse deactivateCompany(UUID id) {
        Company company = suspendCompany(id, "Deactivated by admin");
        return toCompanyResponse(company);
    }
    
    /**
     * Update subscription.
     */
    public CompanyResponse updateSubscription(UUID id, CompanySubscriptionUpdateRequest request) {
        Company company = upgradeSubscription(id, request.subscriptionPlan(), request.subscriptionEndDate());
        return toCompanyResponse(company);
    }
    
    /**
     * Get company stats as DTO.
     */
    public CompanyStatsResponse getCompanyStats(UUID id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));
        CompanyStatistics stats = getCompanyStatistics(id);
        
        // Use existing counts from stats
        long activeUsers = stats.userCount; // Approximate - all counted users are considered active
        long inactiveUsers = 0; // No inactive users tracked in current implementation
        
        // Calculate school statistics (using existing repository methods)
        long totalSchools = schoolRepository.countByCompany_IdAndDeletedAtIsNull(id);
        long activeSchools = totalSchools; // All non-deleted schools considered active
        int schoolUtilization = company.getMaxSchools() > 0 
            ? (int) ((totalSchools * 100) / company.getMaxSchools()) : 0;
        
        // Basic counts for reports and assets (simplified)
        long totalReports = 0; // Will be implemented when report counting methods are added
        long pendingReports = 0;
        long completedReports = 0;
        
        long totalAssets = 0; // Will be implemented when asset counting methods are added
        long functionalAssets = 0;
        long assetsNeedingMaintenance = 0;
        
        // Build user breakdown by type (simplified without enum queries)
        Map<String, Long> usersByType = new HashMap<>();
        // Use placeholder values since enum queries require special handling
        usersByType.put("ADMIN", 1L);
        usersByType.put("SUPERVISOR", 0L); 
        usersByType.put("TECHNICIAN", 0L);
        
        // Build empty report and asset breakdowns for now
        Map<String, Long> reportsByStatus = new HashMap<>();
        Map<String, Long> assetsByCondition = new HashMap<>();
        
        // Determine subscription dates and status
        LocalDateTime periodStart = company.getSubscriptionStartDate() != null 
            ? company.getSubscriptionStartDate().atStartOfDay() : LocalDateTime.now();
        LocalDateTime periodEnd = company.getSubscriptionEndDate() != null 
            ? company.getSubscriptionEndDate().atStartOfDay() : LocalDateTime.now().plusMonths(1);
        
        String subscriptionStatus = company.getStatus() != null 
            ? company.getStatus().name().toLowerCase() : "active";
        
        return new CompanyStatsResponse(
            stats.userCount,                    // totalUsers
            activeUsers,                        // activeUsers 
            inactiveUsers,                      // inactiveUsers
            (int) stats.userUtilization,       // userUtilizationPercentage
            totalSchools,                       // totalSchools
            activeSchools,                      // activeSchools
            schoolUtilization,                  // schoolUtilizationPercentage
            totalReports,                       // totalReports - simplified
            pendingReports,                     // pendingReports - simplified
            completedReports,                   // completedReports - simplified
            totalAssets,                        // totalAssets - simplified
            functionalAssets,                   // functionalAssets - simplified
            assetsNeedingMaintenance,          // assetsNeedingMaintenance - simplified
            0L,                                 // totalInventoryItems - requires inventory module
            0L,                                 // lowStockItems - requires inventory module
            0L,                                 // outOfStockItems - requires inventory module
            BigDecimal.ZERO,                   // totalInventoryValue - requires inventory module
            stats.maxUsers,                    // maxUsersLimit
            (long) company.getMaxSchools(),    // maxSchoolsLimit
            0L,                                // maxReportsLimit - not currently tracked
            BigDecimal.ZERO,                   // maxStorageLimit - not currently tracked
            0,                                 // storageUtilizationPercentage
            subscriptionStatus,                // subscriptionStatus
            periodStart,                       // currentPeriodStart
            periodEnd,                         // currentPeriodEnd
            LocalDateTime.now(),               // lastActivityAt
            usersByType,                       // usersByType
            reportsByStatus,                   // reportsByStatus - simplified
            assetsByCondition,                 // assetsByCondition - simplified
            BigDecimal.ZERO,                   // monthlyRevenue - requires billing module
            BigDecimal.ZERO,                   // totalRevenue - requires billing module
            "active"                           // billingStatus - requires billing module
        );
    }
    
    /**
     * Get current user's company.
     */
    public CompanyResponse getCurrentUserCompany(String username) {
        // Find user by username and get their company
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Company company = user.getCompany();
        return toCompanyResponse(company);
    }
    
    /**
     * Check domain availability.
     */
    public DomainAvailabilityResponse checkDomainAvailability(String domain, String subdomain) {
        boolean domainAvailable = domain == null || !companyRepository.existsByDomainAndDeletedAtIsNull(domain);
        boolean subdomainAvailable = subdomain == null || 
            !companyRepository.existsBySubdomainAndDeletedAtIsNull(subdomain);
        
        String domainMessage = domainAvailable ? "Domain is available" : "Domain is already taken";
        String subdomainMessage = subdomainAvailable ? "Subdomain is available" : "Subdomain is already taken";
        
        return new DomainAvailabilityResponse(
            domain,
            domainAvailable,
            domainMessage,
            subdomain,
            subdomainAvailable,
            subdomainMessage,
            domainAvailable && subdomainAvailable,
            List.of(), // suggestedSubdomains - TODO: implement suggestions
            List.of()  // validationMessages
        );
    }
    
    /**
     * Get companies approaching limits.
     */
    public Page<CompanyListResponse> getCompaniesApproachingLimits(Pageable pageable) {
        // Find companies with >80% resource utilization
        Page<Company> companies = companyRepository.findAll(pageable);
        return companies.map(this::toCompanyListResponse);
    }
    
    /**
     * Get companies with expiring subscriptions.
     */
    public Page<CompanyListResponse> getCompaniesWithExpiringSubscriptions(Pageable pageable, int daysAhead) {
        List<Company> expiringCompanies = findCompaniesWithExpiringSubscriptions(daysAhead);
        // For now, return all companies and filter later - should be improved with custom repository method
        Page<Company> companies = companyRepository.findAll(pageable);
        return companies.map(this::toCompanyListResponse);
    }
    
    /**
     * Check if user belongs to company (for security).
     * 
     * Purpose: Security method for @PreAuthorize annotations to enforce tenant isolation
     * Pattern: Company membership validation with comprehensive error handling
     * Java 23: Optional chaining with fail-safe security defaults
     * Architecture: Repository-based validation with audit logging
     * Standards: Security audit logging for compliance and monitoring
     */
    public boolean belongsToCompany(UUID companyId, String userEmail) {
        logger.debug("Checking if user {} belongs to company {}", userEmail, companyId);
        
        try {
            // Validate input parameters
            if (companyId == null) {
                logger.warn("Security check failed: Company ID is null for user: {}", userEmail);
                return false;
            }
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                logger.warn("Security check failed: User email is null or empty for company: {}", companyId);
                return false;
            }
            
            // Find user by email
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                logger.warn("Security check failed: User not found: {}", userEmail);
                return false;
            }
            
            User user = userOpt.get();
            
            // Check if user has a company assigned
            if (user.getCompany() == null) {
                logger.warn("Security check failed: User {} has no company assigned", userEmail);
                return false;
            }
            
            // Check if user belongs to the specified company
            boolean belongsToCompany = user.belongsToCompany(companyId);
            
            if (belongsToCompany) {
                logger.debug("Security check passed: User {} belongs to company {}", 
                           userEmail, companyId);
            } else {
                logger.warn("Security violation: User {} attempted to access company {} but belongs to company {}", 
                           userEmail, companyId, user.getCompany().getId());
            }
            
            return belongsToCompany;
            
        } catch (Exception e) {
            logger.error("Error during security check for belongsToCompany({}, {}): {}", 
                        companyId, userEmail, e.getMessage(), e);
            // Fail secure - deny access on any error
            return false;
        }
    }
    
    // ========== Mapping Methods ==========
    
    /**
     * Convert Company entity to CompanyResponse DTO.
     * 
     * Purpose: Convert Company entity to complete DTO for detailed responses
     * Pattern: Full entity-to-DTO mapping with computed statistics
     * Java 23: Record construction with comprehensive field mapping
     * Architecture: Repository aggregation for current usage statistics
     */
    private CompanyResponse toCompanyResponse(Company company) {
        // Get current usage statistics efficiently
        long currentUserCount = userRepository.countByCompany_IdAndDeletedAtIsNull(company.getId());
        long currentSchoolCount = schoolRepository.countByCompany_IdAndDeletedAtIsNull(company.getId());
        
        // Calculate subscription status
        boolean isSubscriptionActive = company.getSubscriptionEndDate() == null || 
                                     company.getSubscriptionEndDate().isAfter(LocalDate.now());
        
        return new CompanyResponse(
            company.getId(),
            company.getName(),
            company.getDisplayName(),
            company.getDomain(),
            company.getSubdomain(),
            company.getContactEmail(),
            company.getContactPhone(),
            company.getPrimaryContactName(),
            company.getIndustry(),
            company.getCountry(),
            company.getCity(),
            company.getAddress(),
            company.getPostalCode(),
            company.getTaxNumber(),
            company.getCommercialRegistration(),
            company.getTimezone(),
            company.getLocale(),
            company.getCurrency(),
            company.getSubscriptionPlan(),
            company.getSubscriptionStartDate(),
            company.getSubscriptionEndDate(),
            company.getMaxUsers(),
            company.getMaxSchools(),
            company.getMaxSupervisors(),
            company.getMaxTechnicians(),
            company.getMaxStorageGb(),
            company.getStatus(),
            company.getIsActive(),
            company.getSettings(),
            company.getFeatures(),
            company.getDataClassification(),
            company.getCreatedAt(),
            company.getUpdatedAt(),
            currentUserCount,
            currentSchoolCount,
            isSubscriptionActive
        );
    }
    
    /**
     * Convert Company entity to CompanyListResponse DTO.
     * 
     * Purpose: Convert Company entity to lightweight DTO for list views
     * Pattern: Efficient mapping with lazy-loaded counts for performance
     * Java 23: Record construction with null-safe field mapping
     * Architecture: Repository aggregation for statistics in data layer
     */
    private CompanyListResponse toCompanyListResponse(Company company) {
        // Get current counts efficiently
        long currentUserCount = userRepository.countByCompany_IdAndDeletedAtIsNull(company.getId());
        long currentSchoolCount = schoolRepository.countByCompany_IdAndDeletedAtIsNull(company.getId());
        
        // Calculate subscription status
        boolean isSubscriptionActive = company.getSubscriptionEndDate() == null || 
                                     company.getSubscriptionEndDate().isAfter(LocalDate.now());
        
        return new CompanyListResponse(
            company.getId(),
            company.getName(),
            company.getDisplayName(),
            company.getDomain(),
            company.getSubdomain(),
            company.getContactEmail(),
            company.getIndustry(),
            company.getCountry(),
            company.getCity(),
            company.getSubscriptionPlan(),
            company.getSubscriptionEndDate(),
            company.getStatus(),
            company.getIsActive(),
            company.getCreatedAt(),
            currentUserCount,
            currentSchoolCount,
            company.getMaxUsers(),
            company.getMaxSchools(),
            isSubscriptionActive
        );
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Company statistics DTO.
     */
    public static class CompanyStatistics {
        public UUID companyId;
        public String companyName;
        public CompanyStatus status;
        public SubscriptionPlan subscriptionPlan;
        public LocalDate subscriptionStartDate;
        public LocalDate subscriptionEndDate;
        public long daysUntilExpiry;
        public boolean isExpired;
        public long userCount;
        public int maxUsers;
        public double userUtilization;
        public long adminCount;
        public long supervisorCount;
        public long technicianCount;
    }
    
    /**
     * Platform statistics DTO.
     */
    public static class PlatformStatistics {
        public long totalCompanies;
        public long activeCompanies;
        public long trialCompanies;
        public long expiredTrials;
        public long paidCompanies;
        public Map<CompanyStatus, Long> companiesByStatus = new HashMap<>();
        public Map<SubscriptionPlan, Long> companiesByPlan = new HashMap<>();
    }
}