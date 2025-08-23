package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.data.entity.*;
import com.cafm.cafmbackend.data.enums.AssetCondition;
import com.cafm.cafmbackend.data.enums.AssetStatus;
import com.cafm.cafmbackend.data.repository.*;
import com.cafm.cafmbackend.dto.asset.*;
import com.cafm.cafmbackend.exception.DuplicateResourceException;
import com.cafm.cafmbackend.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for managing assets and equipment.
 * Handles asset lifecycle, maintenance scheduling, and depreciation.
 */
@Service
@Transactional
public class AssetService {
    
    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);
    
    private final AssetRepository assetRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetMaintenanceRepository assetMaintenanceRepository;
    private final SchoolRepository schoolRepository;
    private final CompanyRepository companyRepository;
    
    @Value("${app.asset.depreciation.default-years:5}")
    private int defaultDepreciationYears;
    
    @Value("${app.asset.maintenance.default-frequency-days:90}")
    private int defaultMaintenanceFrequencyDays;
    
    @Value("${app.asset.maintenance.reminder-days-before:7}")
    private int maintenanceReminderDays;
    
    public AssetService(AssetRepository assetRepository,
                       AssetCategoryRepository assetCategoryRepository,
                       AssetMaintenanceRepository assetMaintenanceRepository,
                       SchoolRepository schoolRepository,
                       CompanyRepository companyRepository) {
        this.assetRepository = assetRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.assetMaintenanceRepository = assetMaintenanceRepository;
        this.schoolRepository = schoolRepository;
        this.companyRepository = companyRepository;
    }
    
    // ========== Asset Management ==========
    
    /**
     * Create a new asset.
     */
    public Asset createAsset(Asset asset, UUID companyId) {
        logger.info("Creating new asset: {} for company: {}", asset.getName(), companyId);
        
        // Check for duplicate asset code
        if (assetRepository.existsByAssetCodeAndCompanyId(asset.getAssetCode(), companyId)) {
            throw new DuplicateResourceException("Asset", "assetCode", asset.getAssetCode());
        }
        
        // Check for duplicate serial number if provided
        if (asset.getSerialNumber() != null && 
            assetRepository.existsBySerialNumberAndCompanyId(asset.getSerialNumber(), companyId)) {
            throw new DuplicateResourceException("Asset", "serialNumber", asset.getSerialNumber());
        }
        
        // Set company - fetch reference to avoid detached entity error
        Company company = companyRepository.getReferenceById(companyId);
        asset.setCompany(company);
        
        // Set defaults
        if (asset.getStatus() == null) {
            asset.setStatus(AssetStatus.ACTIVE);
        }
        if (asset.getCondition() == null) {
            asset.setCondition(AssetCondition.GOOD);
        }
        if (asset.getIsActive() == null) {
            asset.setIsActive(true);
        }
        if (asset.getCurrentValue() == null && asset.getPurchaseCost() != null) {
            asset.setCurrentValue(asset.getPurchaseCost());
        }
        
        // Set maintenance schedule if not provided
        if (asset.getMaintenanceFrequencyDays() == null) {
            asset.setMaintenanceFrequencyDays(defaultMaintenanceFrequencyDays);
        }
        if (asset.getNextMaintenanceDate() == null) {
            asset.setNextMaintenanceDate(LocalDate.now().plusDays(asset.getMaintenanceFrequencyDays()));
        }
        
        Asset savedAsset = assetRepository.save(asset);
        logger.info("Asset created successfully: {}", savedAsset.getAssetCode());
        
        return savedAsset;
    }
    
    /**
     * Update asset details.
     */
    public Asset updateAsset(UUID assetId, Asset updatedAsset) {
        logger.info("Updating asset: {}", assetId);
        
        Asset existingAsset = findById(assetId);
        
        // Update allowed fields
        if (updatedAsset.getName() != null) {
            existingAsset.setName(updatedAsset.getName());
        }
        if (updatedAsset.getNameAr() != null) {
            existingAsset.setNameAr(updatedAsset.getNameAr());
        }
        if (updatedAsset.getDescription() != null) {
            existingAsset.setDescription(updatedAsset.getDescription());
        }
        if (updatedAsset.getCategory() != null) {
            existingAsset.setCategory(updatedAsset.getCategory());
        }
        if (updatedAsset.getManufacturer() != null) {
            existingAsset.setManufacturer(updatedAsset.getManufacturer());
        }
        if (updatedAsset.getModel() != null) {
            existingAsset.setModel(updatedAsset.getModel());
        }
        if (updatedAsset.getBarcode() != null) {
            existingAsset.setBarcode(updatedAsset.getBarcode());
        }
        if (updatedAsset.getLocation() != null) {
            existingAsset.setLocation(updatedAsset.getLocation());
        }
        if (updatedAsset.getDepartment() != null) {
            existingAsset.setDepartment(updatedAsset.getDepartment());
        }
        if (updatedAsset.getCondition() != null) {
            existingAsset.setCondition(updatedAsset.getCondition());
        }
        if (updatedAsset.getCurrentValue() != null) {
            existingAsset.setCurrentValue(updatedAsset.getCurrentValue());
        }
        if (updatedAsset.getMaintenanceFrequencyDays() != null) {
            existingAsset.setMaintenanceFrequencyDays(updatedAsset.getMaintenanceFrequencyDays());
        }
        
        return assetRepository.save(existingAsset);
    }
    
    /**
     * Assign asset to user.
     */
    public Asset assignToUser(UUID assetId, UUID userId, String notes) {
        logger.info("Assigning asset {} to user {}", assetId, userId);
        
        Asset asset = findById(assetId);
        
        // Validate asset status
        if (asset.getStatus() != AssetStatus.ACTIVE && asset.getStatus() != AssetStatus.RESERVED) {
            throw new IllegalStateException("Can only assign active or reserved assets. Current status: " + asset.getStatus());
        }
        
        User user = new User();
        user.setId(userId);
        
        asset.setAssignedTo(user);
        asset.setAssignmentDate(LocalDate.now());
        asset.setStatus(AssetStatus.ACTIVE);
        
        return assetRepository.save(asset);
    }
    
    /**
     * Return asset from user.
     */
    public Asset returnFromUser(UUID assetId, String notes) {
        logger.info("Returning asset: {}", assetId);
        
        Asset asset = findById(assetId);
        
        if (asset.getAssignedTo() == null) {
            throw new IllegalStateException("Asset is not assigned to any user");
        }
        
        asset.setAssignedTo(null);
        asset.setAssignmentDate(null);
        asset.setStatus(AssetStatus.ACTIVE);
        
        return assetRepository.save(asset);
    }
    
    /**
     * Transfer asset between locations.
     */
    public Asset transferAsset(UUID assetId, UUID newSchoolId, String newLocation, String notes) {
        logger.info("Transferring asset {} to school {} at location {}", assetId, newSchoolId, newLocation);
        
        Asset asset = findById(assetId);
        
        if (newSchoolId != null) {
            School school = schoolRepository.findById(newSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + newSchoolId));
            asset.setSchool(school);
        }
        
        if (newLocation != null) {
            asset.setLocation(newLocation);
        }
        
        return assetRepository.save(asset);
    }
    
    /**
     * Update asset status.
     */
    public Asset updateStatus(UUID assetId, AssetStatus newStatus, String reason) {
        logger.info("Updating asset {} status to {}: {}", assetId, newStatus, reason);
        
        Asset asset = findById(assetId);
        
        // Validate status transition
        validateStatusTransition(asset.getStatus(), newStatus);
        
        asset.setStatus(newStatus);
        
        // Handle disposal
        if (newStatus == AssetStatus.DISPOSED) {
            asset.setDisposalDate(LocalDate.now());
            asset.setDisposalReason(reason);
            asset.setIsActive(false);
        }
        
        return assetRepository.save(asset);
    }
    
    /**
     * Dispose of an asset.
     */
    public Asset disposeAsset(UUID assetId, String method, BigDecimal disposalValue, String reason) {
        logger.info("Disposing asset {}: method={}, value={}", assetId, method, disposalValue);
        
        Asset asset = findById(assetId);
        
        asset.setStatus(AssetStatus.DISPOSED);
        asset.setDisposalDate(LocalDate.now());
        asset.setDisposalMethod(method);
        asset.setDisposalValue(disposalValue);
        asset.setDisposalReason(reason);
        asset.setIsActive(false);
        
        return assetRepository.save(asset);
    }
    
    // ========== Maintenance Management ==========
    
    /**
     * Record maintenance for an asset.
     */
    public AssetMaintenance recordMaintenance(UUID assetId, AssetMaintenance maintenance) {
        logger.info("Recording maintenance for asset: {}", assetId);
        
        Asset asset = findById(assetId);
        
        maintenance.setAsset(asset);
        AssetMaintenance savedMaintenance = assetMaintenanceRepository.save(maintenance);
        
        // Update asset maintenance dates
        asset.setLastMaintenanceDate(maintenance.getMaintenanceDate());
        asset.setNextMaintenanceDate(maintenance.getMaintenanceDate()
            .plusDays(asset.getMaintenanceFrequencyDays()));
        
        // Note: AssetMaintenance entity doesn't have a getCost() method
        // Would need to add cost field to AssetMaintenance entity
        
        assetRepository.save(asset);
        
        return savedMaintenance;
    }
    
    /**
     * Schedule maintenance for an asset.
     */
    public Asset scheduleMaintenance(UUID assetId, LocalDate scheduledDate) {
        logger.info("Scheduling maintenance for asset {} on {}", assetId, scheduledDate);
        
        Asset asset = findById(assetId);
        asset.setNextMaintenanceDate(scheduledDate);
        
        return assetRepository.save(asset);
    }
    
    /**
     * Get maintenance history for an asset.
     */
    public List<AssetMaintenance> getMaintenanceHistory(UUID assetId) {
        return assetMaintenanceRepository.findByAssetIdOrderByMaintenanceDateDesc(assetId);
    }
    
    /**
     * Find assets due for maintenance.
     */
    public List<Asset> findAssetsDueForMaintenance(UUID companyId, int daysAhead) {
        LocalDate dueDate = LocalDate.now().plusDays(daysAhead);
        return assetRepository.findAssetsDueForMaintenance(companyId, dueDate);
    }
    
    // ========== Depreciation Calculation ==========
    
    /**
     * Calculate depreciation for an asset.
     */
    public BigDecimal calculateDepreciation(UUID assetId) {
        Asset asset = findById(assetId);
        
        if (asset.getPurchaseCost() == null || asset.getPurchaseDate() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal salvageValue = asset.getSalvageValue() != null ? 
            asset.getSalvageValue() : BigDecimal.ZERO;
        
        String method = asset.getDepreciationMethod() != null ? 
            asset.getDepreciationMethod() : "straight_line";
        
        return switch (method) {
            case "straight_line" -> calculateStraightLineDepreciation(
                asset.getPurchaseCost(), salvageValue, asset.getPurchaseDate()
            );
            case "declining_balance" -> calculateDecliningBalanceDepreciation(
                asset.getPurchaseCost(), salvageValue, asset.getPurchaseDate()
            );
            default -> BigDecimal.ZERO;
        };
    }
    
    /**
     * Calculate straight-line depreciation.
     */
    private BigDecimal calculateStraightLineDepreciation(BigDecimal purchaseCost, 
                                                        BigDecimal salvageValue, 
                                                        LocalDate purchaseDate) {
        long monthsOwned = ChronoUnit.MONTHS.between(purchaseDate, LocalDate.now());
        if (monthsOwned <= 0) return BigDecimal.ZERO;
        
        BigDecimal depreciableAmount = purchaseCost.subtract(salvageValue);
        BigDecimal monthlyDepreciation = depreciableAmount.divide(
            BigDecimal.valueOf(defaultDepreciationYears * 12), 2, RoundingMode.HALF_UP
        );
        
        BigDecimal totalDepreciation = monthlyDepreciation.multiply(BigDecimal.valueOf(monthsOwned));
        
        // Ensure we don't depreciate below salvage value
        if (totalDepreciation.compareTo(depreciableAmount) > 0) {
            return depreciableAmount;
        }
        
        return totalDepreciation;
    }
    
    /**
     * Calculate declining balance depreciation.
     */
    private BigDecimal calculateDecliningBalanceDepreciation(BigDecimal purchaseCost, 
                                                            BigDecimal salvageValue, 
                                                            LocalDate purchaseDate) {
        long yearsOwned = ChronoUnit.YEARS.between(purchaseDate, LocalDate.now());
        if (yearsOwned <= 0) return BigDecimal.ZERO;
        
        double depreciationRate = 2.0 / defaultDepreciationYears; // Double declining balance
        BigDecimal currentValue = purchaseCost;
        
        for (int i = 0; i < yearsOwned; i++) {
            BigDecimal yearlyDepreciation = currentValue.multiply(
                BigDecimal.valueOf(depreciationRate)
            );
            currentValue = currentValue.subtract(yearlyDepreciation);
            
            // Don't depreciate below salvage value
            if (currentValue.compareTo(salvageValue) < 0) {
                currentValue = salvageValue;
                break;
            }
        }
        
        return purchaseCost.subtract(currentValue);
    }
    
    /**
     * Update current values for all assets based on depreciation.
     */
    @Transactional
    public void updateAssetValues(UUID companyId) {
        logger.info("Updating asset values for company: {}", companyId);
        
        List<Asset> assets = assetRepository.findByCompanyIdAndStatus(companyId, AssetStatus.ACTIVE);
        
        for (Asset asset : assets) {
            BigDecimal depreciation = calculateDepreciation(asset.getId());
            BigDecimal currentValue = asset.getPurchaseCost().subtract(depreciation);
            
            if (currentValue.compareTo(BigDecimal.ZERO) < 0) {
                currentValue = BigDecimal.ZERO;
            }
            
            asset.setCurrentValue(currentValue);
            assetRepository.save(asset);
        }
        
        logger.info("Updated values for {} assets", assets.size());
    }
    
    // ========== Query Methods ==========
    
    /**
     * Find asset by ID.
     */
    public Asset findById(UUID assetId) {
        return assetRepository.findById(assetId)
            .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
    }
    
    /**
     * Find asset by code.
     */
    public Optional<Asset> findByAssetCode(String assetCode, UUID companyId) {
        return assetRepository.findByAssetCodeAndCompanyId(assetCode, companyId);
    }
    
    /**
     * Find asset by serial number.
     */
    public Optional<Asset> findBySerialNumber(String serialNumber, UUID companyId) {
        return assetRepository.findBySerialNumberAndCompanyId(serialNumber, companyId);
    }
    
    /**
     * Find all assets for a company.
     */
    public Page<Asset> findByCompany(UUID companyId, Pageable pageable) {
        return assetRepository.findByCompanyId(companyId, pageable);
    }
    
    /**
     * Find assets by status.
     */
    public List<Asset> findByStatus(UUID companyId, AssetStatus status) {
        return assetRepository.findByCompanyIdAndStatus(companyId, status);
    }
    
    /**
     * Find assets by condition.
     */
    public List<Asset> findByCondition(UUID companyId, AssetCondition condition) {
        return assetRepository.findByCompanyIdAndCondition(companyId, condition);
    }
    
    /**
     * Find assets by school.
     */
    public Page<Asset> findBySchool(UUID schoolId, Pageable pageable) {
        // Note: There's no specific method for finding by school in AssetRepository
        // Would need to add this query or use a specification
        return Page.empty(pageable);
    }
    
    /**
     * Find assets assigned to user.
     */
    public List<Asset> findAssignedToUser(UUID userId) {
        return assetRepository.findByAssignedToId(userId);
    }
    
    /**
     * Find assets under warranty.
     */
    public List<Asset> findAssetsUnderWarranty(UUID companyId) {
        // Assets with warranty expiring in the future
        LocalDate today = LocalDate.now();
        LocalDate farFuture = today.plusYears(10);
        return assetRepository.findExpiringWarranties(companyId, today, farFuture);
    }
    
    /**
     * Find assets with expiring warranty.
     */
    public List<Asset> findAssetsWithExpiringWarranty(UUID companyId, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(daysAhead);
        return assetRepository.findExpiringWarranties(companyId, today, futureDate);
    }
    
    /**
     * Search assets.
     */
    public Page<Asset> searchAssets(UUID companyId, String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findByCompany(companyId, pageable);
        }
        return assetRepository.searchAssets(companyId, searchTerm, pageable);
    }
    
    // ========== Category Management ==========
    
    /**
     * Create asset category.
     */
    public AssetCategory createCategory(AssetCategory category, UUID companyId) {
        logger.info("Creating asset category: {}", category.getName());
        
        // Check for duplicate name
        if (assetCategoryRepository.existsByNameAndCompanyId(category.getName(), companyId)) {
            throw new DuplicateResourceException("AssetCategory", "name", category.getName());
        }
        
        Company company = companyRepository.getReferenceById(companyId);
        category.setCompany(company);
        
        return assetCategoryRepository.save(category);
    }
    
    /**
     * Get all categories for company.
     */
    public Page<AssetCategory> getCategories(UUID companyId, Pageable pageable) {
        return assetCategoryRepository.findByCompanyIdAndIsActiveTrue(companyId, pageable);
    }
    
    // ========== Statistics Methods ==========
    
    /**
     * Get asset statistics for company.
     */
    @Transactional(readOnly = true)
    public AssetStatistics getStatistics(UUID companyId) {
        AssetStatistics stats = new AssetStatistics();
        
        List<Asset> allAssets = assetRepository.findByCompanyId(companyId, Pageable.unpaged()).getContent();
        
        stats.totalAssets = allAssets.size();
        stats.activeAssets = allAssets.stream().filter(a -> a.getStatus() == AssetStatus.ACTIVE).count();
        stats.inUseAssets = allAssets.stream().filter(a -> a.getStatus() == AssetStatus.RESERVED).count();
        stats.maintenanceAssets = allAssets.stream().filter(a -> a.getStatus() == AssetStatus.MAINTENANCE).count();
        stats.disposedAssets = allAssets.stream().filter(a -> a.getStatus() == AssetStatus.DISPOSED).count();
        
        // Calculate total values
        stats.totalPurchaseValue = allAssets.stream()
            .map(Asset::getPurchaseCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.totalCurrentValue = allAssets.stream()
            .map(Asset::getCurrentValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.totalDepreciation = stats.totalPurchaseValue.subtract(stats.totalCurrentValue);
        
        // Maintenance statistics
        stats.totalMaintenanceCost = allAssets.stream()
            .map(Asset::getTotalMaintenanceCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.assetsDueForMaintenance = allAssets.stream()
            .filter(Asset::isMaintenanceDue)
            .count();
        
        // Warranty statistics
        stats.assetsUnderWarranty = allAssets.stream()
            .filter(Asset::isUnderWarranty)
            .count();
        
        // Condition breakdown
        stats.assetsByCondition = allAssets.stream()
            .filter(a -> a.getCondition() != null)
            .collect(Collectors.groupingBy(Asset::getCondition, Collectors.counting()));
        
        // Category breakdown
        stats.assetsByCategory = allAssets.stream()
            .filter(a -> a.getCategory() != null)
            .collect(Collectors.groupingBy(
                a -> a.getCategory().getName(),
                Collectors.counting()
            ));
        
        // Age analysis
        stats.averageAgeYears = allAssets.stream()
            .mapToDouble(Asset::getAgeInYears)
            .average()
            .orElse(0.0);
        
        return stats;
    }
    
    /**
     * Get asset valuation report.
     */
    @Transactional(readOnly = true)
    public AssetValuationReport getValuationReport(UUID companyId) {
        AssetValuationReport report = new AssetValuationReport();
        report.reportDate = LocalDate.now();
        
        List<Asset> assets = assetRepository.findByCompanyIdAndStatus(companyId, AssetStatus.ACTIVE);
        
        for (Asset asset : assets) {
            AssetValuationEntry entry = new AssetValuationEntry();
            entry.assetId = asset.getId();
            entry.assetCode = asset.getAssetCode();
            entry.assetName = asset.getName();
            entry.purchaseDate = asset.getPurchaseDate();
            entry.purchaseCost = asset.getPurchaseCost();
            entry.currentValue = asset.getCurrentValue();
            entry.depreciation = calculateDepreciation(asset.getId());
            entry.depreciationRate = calculateDepreciationRate(asset);
            
            report.entries.add(entry);
            report.totalPurchaseCost = report.totalPurchaseCost.add(
                entry.purchaseCost != null ? entry.purchaseCost : BigDecimal.ZERO
            );
            report.totalCurrentValue = report.totalCurrentValue.add(
                entry.currentValue != null ? entry.currentValue : BigDecimal.ZERO
            );
            report.totalDepreciation = report.totalDepreciation.add(
                entry.depreciation != null ? entry.depreciation : BigDecimal.ZERO
            );
        }
        
        report.assetCount = report.entries.size();
        
        return report;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Validate status transition.
     */
    private void validateStatusTransition(AssetStatus currentStatus, AssetStatus newStatus) {
        // Define valid transitions
        Map<AssetStatus, Set<AssetStatus>> validTransitions = Map.of(
            AssetStatus.ACTIVE, Set.of(AssetStatus.RESERVED, AssetStatus.MAINTENANCE, AssetStatus.RETIRED, AssetStatus.DISPOSED),
            AssetStatus.RESERVED, Set.of(AssetStatus.ACTIVE, AssetStatus.MAINTENANCE, AssetStatus.DISPOSED),
            AssetStatus.MAINTENANCE, Set.of(AssetStatus.ACTIVE, AssetStatus.RESERVED, AssetStatus.DISPOSED),
            AssetStatus.RETIRED, Set.of(AssetStatus.ACTIVE, AssetStatus.DISPOSED),
            AssetStatus.DISPOSED, Set.of() // No transitions from disposed
        );
        
        Set<AssetStatus> allowedStatuses = validTransitions.get(currentStatus);
        if (allowedStatuses != null && !allowedStatuses.contains(newStatus)) {
            throw new IllegalStateException(String.format(
                "Invalid status transition from %s to %s", currentStatus, newStatus
            ));
        }
    }
    
    /**
     * Calculate depreciation rate for an asset.
     */
    private BigDecimal calculateDepreciationRate(Asset asset) {
        if (asset.getPurchaseCost() == null || asset.getPurchaseCost().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal depreciation = calculateDepreciation(asset.getId());
        return depreciation.divide(asset.getPurchaseCost(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Generate unique asset code.
     */
    public String generateAssetCode(String prefix, UUID companyId) {
        String baseCode = prefix.toUpperCase() + "-" + System.currentTimeMillis();
        String assetCode = baseCode;
        int counter = 1;
        
        while (assetRepository.existsByAssetCodeAndCompanyId(assetCode, companyId)) {
            assetCode = baseCode + "-" + counter++;
        }
        
        return assetCode;
    }
    
    // ========== DTO-Based Methods ==========
    
    /**
     * Create a new asset from DTO.
     */
    public AssetResponse createAssetFromDto(AssetCreateRequest request, UUID companyId) {
        logger.info("Creating new asset with code: {}", request.assetCode());
        
        // Validate asset code uniqueness
        if (assetRepository.existsByAssetCodeAndCompanyId(request.assetCode(), companyId)) {
            throw new DuplicateResourceException("Asset", "assetCode", request.assetCode());
        }
        
        // Validate serial number uniqueness if provided
        if (request.serialNumber() != null && 
            assetRepository.existsBySerialNumberAndCompanyId(request.serialNumber(), companyId)) {
            throw new DuplicateResourceException("Asset", "serialNumber", request.serialNumber());
        }
        
        // Create asset entity from DTO
        Asset asset = mapCreateRequestToEntity(request, companyId);
        
        Asset savedAsset = assetRepository.save(asset);
        logger.info("Asset created successfully with ID: {}", savedAsset.getId());
        
        return mapToResponse(savedAsset);
    }
    
    /**
     * Update an existing asset from DTO.
     */
    public AssetResponse updateAssetFromDto(UUID assetId, AssetUpdateRequest request) {
        logger.info("Updating asset: {}", assetId);
        
        Asset existingAsset = findById(assetId);
        
        // Apply updates from DTO
        mapUpdateRequestToEntity(request, existingAsset);
        
        Asset savedAsset = assetRepository.save(existingAsset);
        return mapToResponse(savedAsset);
    }
    
    /**
     * Get asset by ID as DTO.
     */
    public AssetResponse getAssetByIdAsDto(UUID assetId) {
        Asset asset = findById(assetId);
        return mapToResponse(asset);
    }
    
    /**
     * Get all assets with filtering and pagination.
     */
    public Page<AssetListResponse> getAllAssetsAsDto(Pageable pageable, UUID companyId, AssetStatus status, 
                                                    AssetCondition condition, UUID categoryId, UUID schoolId, 
                                                    String search, Boolean isActive) {
        
        Specification<Asset> spec = Specification.where(null);
        
        // Company filter (mandatory for multi-tenancy)
        spec = spec.and((root, query, cb) -> cb.equal(root.get("company").get("id"), companyId));
        
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        
        if (condition != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("condition"), condition));
        }
        
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        
        if (schoolId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("school").get("id"), schoolId));
        }
        
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }
        
        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("assetCode")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("nameAr")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("serialNumber")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("manufacturer")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("model")), "%" + search.toLowerCase() + "%")
                )
            );
        }
        
        Page<Asset> assets = assetRepository.findAll(spec, pageable);
        return assets.map(this::mapToListResponse);
    }
    
    /**
     * Get assets statistics as DTO.
     */
    public AssetStatsResponse getAssetsStatsAsDto(UUID companyId) {
        List<Asset> assets = assetRepository.findByCompanyId(companyId, Pageable.unpaged()).getContent();
        return buildAssetStatsResponse(assets);
    }
    
    // ========== DTO Mapping Methods ==========
    
    /**
     * Map AssetCreateRequest to Asset entity.
     */
    private Asset mapCreateRequestToEntity(AssetCreateRequest request, UUID companyId) {
        Asset asset = new Asset();
        
        // Set company - fetch reference to avoid detached entity error
        Company company = companyRepository.getReferenceById(companyId);
        asset.setCompany(company);
        
        // Basic information
        asset.setAssetCode(request.assetCode());
        asset.setName(request.name());
        asset.setNameAr(request.nameAr());
        asset.setDescription(request.description());
        asset.setManufacturer(request.manufacturer());
        asset.setModel(request.model());
        asset.setSerialNumber(request.serialNumber());
        asset.setBarcode(request.barcode());
        
        // Purchase information
        asset.setPurchaseDate(request.purchaseDate());
        asset.setPurchaseOrderNumber(request.purchaseOrderNumber());
        asset.setSupplier(request.supplier());
        asset.setWarrantyStartDate(request.warrantyStartDate());
        asset.setWarrantyEndDate(request.warrantyEndDate());
        
        // Financial information
        asset.setPurchaseCost(request.purchaseCost());
        asset.setCurrentValue(request.getEffectiveCurrentValue());
        asset.setSalvageValue(request.salvageValue());
        asset.setDepreciationMethod(request.getEffectiveDepreciationMethod());
        
        // Location and assignment
        if (request.schoolId() != null) {
            School school = schoolRepository.getReferenceById(request.schoolId());
            asset.setSchool(school);
        }
        asset.setDepartment(request.department());
        asset.setLocation(request.location());
        
        if (request.assignedToId() != null) {
            User user = new User();
            user.setId(request.assignedToId());
            asset.setAssignedTo(user);
            asset.setAssignmentDate(LocalDate.now());
        }
        
        // Category
        if (request.categoryId() != null) {
            AssetCategory category = new AssetCategory();
            category.setId(request.categoryId());
            asset.setCategory(category);
        }
        
        // Maintenance
        asset.setMaintenanceFrequencyDays(request.getEffectiveMaintenanceFrequencyDays());
        asset.setNextMaintenanceDate(request.getEffectiveNextMaintenanceDate());
        
        // Status and condition
        asset.setStatus(request.getEffectiveStatus());
        asset.setCondition(request.getEffectiveCondition());
        asset.setIsActive(true);
        
        return asset;
    }
    
    /**
     * Map AssetUpdateRequest to existing Asset entity.
     */
    private void mapUpdateRequestToEntity(AssetUpdateRequest request, Asset asset) {
        if (request.name() != null) {
            asset.setName(request.name());
        }
        if (request.nameAr() != null) {
            asset.setNameAr(request.nameAr());
        }
        if (request.description() != null) {
            asset.setDescription(request.description());
        }
        if (request.manufacturer() != null) {
            asset.setManufacturer(request.manufacturer());
        }
        if (request.model() != null) {
            asset.setModel(request.model());
        }
        if (request.barcode() != null) {
            asset.setBarcode(request.barcode());
        }
        if (request.purchaseOrderNumber() != null) {
            asset.setPurchaseOrderNumber(request.purchaseOrderNumber());
        }
        if (request.supplier() != null) {
            asset.setSupplier(request.supplier());
        }
        if (request.warrantyStartDate() != null) {
            asset.setWarrantyStartDate(request.warrantyStartDate());
        }
        if (request.warrantyEndDate() != null) {
            asset.setWarrantyEndDate(request.warrantyEndDate());
        }
        if (request.currentValue() != null) {
            asset.setCurrentValue(request.currentValue());
        }
        if (request.salvageValue() != null) {
            asset.setSalvageValue(request.salvageValue());
        }
        if (request.depreciationMethod() != null) {
            asset.setDepreciationMethod(request.depreciationMethod());
        }
        if (request.schoolId() != null) {
            School school = schoolRepository.getReferenceById(request.schoolId());
            asset.setSchool(school);
        }
        if (request.department() != null) {
            asset.setDepartment(request.department());
        }
        if (request.location() != null) {
            asset.setLocation(request.location());
        }
        if (request.categoryId() != null) {
            AssetCategory category = new AssetCategory();
            category.setId(request.categoryId());
            asset.setCategory(category);
        }
        if (request.maintenanceFrequencyDays() != null) {
            asset.setMaintenanceFrequencyDays(request.maintenanceFrequencyDays());
        }
        if (request.nextMaintenanceDate() != null) {
            asset.setNextMaintenanceDate(request.nextMaintenanceDate());
        }
        if (request.status() != null) {
            asset.setStatus(request.status());
        }
        if (request.condition() != null) {
            asset.setCondition(request.condition());
        }
        if (request.isActive() != null) {
            asset.setIsActive(request.isActive());
        }
    }
    
    /**
     * Map Asset entity to AssetResponse DTO.
     */
    private AssetResponse mapToResponse(Asset asset) {
        return new AssetResponse(
            asset.getId(),
            asset.getAssetCode(),
            asset.getName(),
            asset.getNameAr(),
            asset.getDescription(),
            asset.getManufacturer(),
            asset.getModel(),
            asset.getSerialNumber(),
            asset.getBarcode(),
            asset.getPurchaseDate(),
            asset.getPurchaseOrderNumber(),
            asset.getSupplier(),
            asset.getWarrantyStartDate(),
            asset.getWarrantyEndDate(),
            asset.getPurchaseCost(),
            asset.getCurrentValue(),
            asset.getSalvageValue(),
            asset.getDepreciationMethod(),
            asset.getSchool() != null ? asset.getSchool().getId() : null,
            asset.getSchool() != null ? asset.getSchool().getName() : null,
            asset.getDepartment(),
            asset.getLocation(),
            asset.getAssignedTo() != null ? asset.getAssignedTo().getId() : null,
            asset.getAssignedTo() != null ? asset.getAssignedTo().getUsername() : null, // TODO: Get actual name
            asset.getAssignmentDate(),
            asset.getLastMaintenanceDate(),
            asset.getNextMaintenanceDate(),
            asset.getMaintenanceFrequencyDays(),
            asset.getTotalMaintenanceCost(),
            asset.getStatus(),
            asset.getCondition(),
            asset.getIsActive(),
            asset.getCategory() != null ? asset.getCategory().getId() : null,
            asset.getCategory() != null ? asset.getCategory().getName() : null,
            asset.getDisposalDate(),
            asset.getDisposalMethod(),
            asset.getDisposalValue(),
            asset.getDisposalReason(),
            asset.getCompany() != null ? asset.getCompany().getId() : null,
            asset.getCreatedAt(),
            asset.getUpdatedAt(),
            asset.getAgeInYears(),
            asset.isUnderWarranty(),
            asset.getWarrantyDaysRemaining(),
            asset.isMaintenanceDue(),
            asset.getDaysUntilMaintenance(),
            calculateDepreciation(asset.getId()),
            calculateDepreciationRate(asset)
        );
    }
    
    /**
     * Map Asset entity to AssetListResponse DTO.
     */
    private AssetListResponse mapToListResponse(Asset asset) {
        return new AssetListResponse(
            asset.getId(),
            asset.getAssetCode(),
            asset.getName(),
            asset.getNameAr(),
            asset.getManufacturer(),
            asset.getModel(),
            asset.getSerialNumber(),
            asset.getCategory() != null ? asset.getCategory().getName() : null,
            asset.getSchool() != null ? asset.getSchool().getName() : null,
            asset.getDepartment(),
            asset.getLocation(),
            asset.getAssignedTo() != null ? asset.getAssignedTo().getUsername() : null, // TODO: Get actual name
            asset.getPurchaseDate(),
            asset.getPurchaseCost(),
            asset.getCurrentValue(),
            asset.getStatus(),
            asset.getCondition(),
            asset.getIsActive(),
            asset.getCreatedAt(),
            asset.isUnderWarranty(),
            asset.isMaintenanceDue(),
            asset.getDaysUntilMaintenance(),
            asset.getAgeInYears(),
            calculateDepreciationRate(asset)
        );
    }
    
    /**
     * Build comprehensive asset statistics response.
     */
    private AssetStatsResponse buildAssetStatsResponse(List<Asset> assets) {
        // Basic counts
        long totalAssets = assets.size();
        Map<AssetStatus, Long> statusCounts = assets.stream()
            .collect(Collectors.groupingBy(Asset::getStatus, Collectors.counting()));
        
        long activeAssets = statusCounts.getOrDefault(AssetStatus.ACTIVE, 0L);
        long reservedAssets = statusCounts.getOrDefault(AssetStatus.RESERVED, 0L);
        long maintenanceAssets = statusCounts.getOrDefault(AssetStatus.MAINTENANCE, 0L);
        long retiredAssets = statusCounts.getOrDefault(AssetStatus.RETIRED, 0L);
        long disposedAssets = statusCounts.getOrDefault(AssetStatus.DISPOSED, 0L);
        
        // Condition counts
        Map<AssetCondition, Long> conditionCounts = assets.stream()
            .filter(a -> a.getCondition() != null)
            .collect(Collectors.groupingBy(Asset::getCondition, Collectors.counting()));
        
        long excellentCondition = conditionCounts.getOrDefault(AssetCondition.EXCELLENT, 0L);
        long goodCondition = conditionCounts.getOrDefault(AssetCondition.GOOD, 0L);
        long fairCondition = conditionCounts.getOrDefault(AssetCondition.FAIR, 0L);
        long poorCondition = conditionCounts.getOrDefault(AssetCondition.POOR, 0L);
        long unusableCondition = conditionCounts.getOrDefault(AssetCondition.UNUSABLE, 0L);
        
        // Financial metrics
        BigDecimal totalPurchaseValue = assets.stream()
            .map(Asset::getPurchaseCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCurrentValue = assets.stream()
            .map(Asset::getCurrentValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDepreciation = totalPurchaseValue.subtract(totalCurrentValue);
        
        BigDecimal averageCost = totalAssets > 0 && totalPurchaseValue.compareTo(BigDecimal.ZERO) > 0 ?
            totalPurchaseValue.divide(BigDecimal.valueOf(totalAssets), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        
        BigDecimal totalMaintenanceCost = assets.stream()
            .map(Asset::getTotalMaintenanceCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageMaintenanceCost = totalAssets > 0 && totalMaintenanceCost.compareTo(BigDecimal.ZERO) > 0 ?
            totalMaintenanceCost.divide(BigDecimal.valueOf(totalAssets), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        
        // Maintenance metrics
        long assetsDueForMaintenance = assets.stream()
            .filter(Asset::isMaintenanceDue)
            .count();
        
        long overdueMaintenanceAssets = assets.stream()
            .filter(a -> a.isMaintenanceDue() && a.getNextMaintenanceDate() != null && 
                        a.getNextMaintenanceDate().isBefore(LocalDate.now().minusDays(7)))
            .count();
        
        long assetsWithoutMaintenanceHistory = assets.stream()
            .filter(a -> a.getLastMaintenanceDate() == null)
            .count();
        
        // Warranty metrics
        long assetsUnderWarranty = assets.stream()
            .filter(Asset::isUnderWarranty)
            .count();
        
        long assetsWithExpiredWarranty = assets.stream()
            .filter(a -> a.getWarrantyEndDate() != null && !a.isUnderWarranty())
            .count();
        
        long assetsWithoutWarrantyInfo = assets.stream()
            .filter(a -> a.getWarrantyEndDate() == null)
            .count();
        
        // Assignment metrics
        long assignedAssets = assets.stream()
            .filter(a -> a.getAssignedTo() != null)
            .count();
        
        long unassignedAssets = totalAssets - assignedAssets;
        
        BigDecimal utilizationPercentage = totalAssets > 0 ?
            BigDecimal.valueOf(assignedAssets * 100.0 / totalAssets).setScale(1, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        
        // Age analysis
        double averageAge = assets.stream()
            .mapToDouble(Asset::getAgeInYears)
            .average()
            .orElse(0.0);
        
        long newAssets = assets.stream().filter(a -> a.getAgeInYears() < 1).count();
        long recentAssets = assets.stream().filter(a -> a.getAgeInYears() >= 1 && a.getAgeInYears() < 3).count();
        long matureAssets = assets.stream().filter(a -> a.getAgeInYears() >= 3 && a.getAgeInYears() < 5).count();
        long oldAssets = assets.stream().filter(a -> a.getAgeInYears() >= 5).count();
        
        // Category breakdown
        Map<String, Long> assetsByCategory = assets.stream()
            .filter(a -> a.getCategory() != null)
            .collect(Collectors.groupingBy(a -> a.getCategory().getName(), Collectors.counting()));
        
        Map<String, BigDecimal> valueByCategory = assets.stream()
            .filter(a -> a.getCategory() != null && a.getPurchaseCost() != null)
            .collect(Collectors.groupingBy(
                a -> a.getCategory().getName(),
                Collectors.reducing(BigDecimal.ZERO, Asset::getPurchaseCost, BigDecimal::add)
            ));
        
        // Location breakdown
        Map<String, Long> assetsBySchool = assets.stream()
            .filter(a -> a.getSchool() != null)
            .collect(Collectors.groupingBy(a -> a.getSchool().getName(), Collectors.counting()));
        
        Map<String, Long> assetsByDepartment = assets.stream()
            .filter(a -> a.getDepartment() != null)
            .collect(Collectors.groupingBy(Asset::getDepartment, Collectors.counting()));
        
        // Risk indicators
        long highRiskAssets = poorCondition + unusableCondition;
        long assetsNeedingAttention = assetsDueForMaintenance + highRiskAssets;
        
        // Calculate health scores (simplified)
        int overallHealthScore = calculateOverallHealthScore(assets);
        int maintenanceEfficiencyScore = calculateMaintenanceEfficiencyScore(assets);
        int financialPerformanceScore = calculateFinancialPerformanceScore(assets);
        
        return new AssetStatsResponse(
            totalAssets, activeAssets, reservedAssets, maintenanceAssets, retiredAssets, disposedAssets,
            excellentCondition, goodCondition, fairCondition, poorCondition, unusableCondition,
            totalPurchaseValue, totalCurrentValue, totalDepreciation, averageCost,
            totalMaintenanceCost, averageMaintenanceCost,
            assetsDueForMaintenance, overdueMaintenanceAssets, assetsWithoutMaintenanceHistory,
            90.0, // default average maintenance frequency
            assetsUnderWarranty, assetsWithExpiredWarranty, assetsWithoutWarrantyInfo, 0L, // warranties expiring soon
            assignedAssets, unassignedAssets, utilizationPercentage,
            averageAge, newAssets, recentAssets, matureAssets, oldAssets,
            assetsByCategory, valueByCategory, assetsBySchool, assetsByDepartment,
            0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, // purchase trends
            0L, BigDecimal.ZERO, // disposal metrics
            highRiskAssets, assetsNeedingAttention, 0L, // compliance issues
            overallHealthScore, maintenanceEfficiencyScore, financialPerformanceScore,
            BigDecimal.ZERO, BigDecimal.ZERO, "stable", // trends
            LocalDate.now()
        );
    }
    
    private int calculateOverallHealthScore(List<Asset> assets) {
        if (assets.isEmpty()) return 0;
        
        double averageScore = assets.stream()
            .mapToInt(asset -> {
                int score = 100;
                if (asset.getCondition() != null) {
                    score -= switch (asset.getCondition()) {
                        case EXCELLENT -> 0;
                        case GOOD -> 10;
                        case FAIR -> 25;
                        case POOR -> 40;
                        case UNUSABLE -> 60;
                    };
                }
                if (asset.isMaintenanceDue()) score -= 20;
                if (!asset.isUnderWarranty()) score -= 10;
                return Math.max(0, score);
            })
            .average()
            .orElse(0.0);
        
        return (int) Math.round(averageScore);
    }
    
    private int calculateMaintenanceEfficiencyScore(List<Asset> assets) {
        if (assets.isEmpty()) return 0;
        
        long totalAssets = assets.size();
        long assetsDue = assets.stream().filter(Asset::isMaintenanceDue).count();
        long assetsWithHistory = assets.stream().filter(a -> a.getLastMaintenanceDate() != null).count();
        
        double efficiency = ((double) (totalAssets - assetsDue) / totalAssets) * 0.7 +
                           ((double) assetsWithHistory / totalAssets) * 0.3;
        
        return (int) Math.round(efficiency * 100);
    }
    
    private int calculateFinancialPerformanceScore(List<Asset> assets) {
        if (assets.isEmpty()) return 0;
        
        BigDecimal totalPurchase = assets.stream()
            .map(Asset::getPurchaseCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCurrent = assets.stream()
            .map(Asset::getCurrentValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalPurchase.compareTo(BigDecimal.ZERO) == 0) return 0;
        
        double retentionRate = totalCurrent.divide(totalPurchase, 4, RoundingMode.HALF_UP).doubleValue();
        
        return (int) Math.round(retentionRate * 100);
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Asset statistics DTO.
     */
    public static class AssetStatistics {
        public long totalAssets;
        public long activeAssets;
        public long inUseAssets;
        public long maintenanceAssets;
        public long disposedAssets;
        public BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        public BigDecimal totalCurrentValue = BigDecimal.ZERO;
        public BigDecimal totalDepreciation = BigDecimal.ZERO;
        public BigDecimal totalMaintenanceCost = BigDecimal.ZERO;
        public long assetsDueForMaintenance;
        public long assetsUnderWarranty;
        public Map<AssetCondition, Long> assetsByCondition = new HashMap<>();
        public Map<String, Long> assetsByCategory = new HashMap<>();
        public double averageAgeYears;
    }
    
    /**
     * Asset valuation report DTO.
     */
    public static class AssetValuationReport {
        public LocalDate reportDate;
        public int assetCount;
        public BigDecimal totalPurchaseCost = BigDecimal.ZERO;
        public BigDecimal totalCurrentValue = BigDecimal.ZERO;
        public BigDecimal totalDepreciation = BigDecimal.ZERO;
        public List<AssetValuationEntry> entries = new ArrayList<>();
    }
    
    /**
     * Asset valuation entry.
     */
    public static class AssetValuationEntry {
        public UUID assetId;
        public String assetCode;
        public String assetName;
        public LocalDate purchaseDate;
        public BigDecimal purchaseCost;
        public BigDecimal currentValue;
        public BigDecimal depreciation;
        public BigDecimal depreciationRate;
    }
}