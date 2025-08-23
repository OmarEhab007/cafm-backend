# Spring Boot Backend Code Standards and Conventions

This document defines comprehensive, market-standard coding conventions for Spring Boot backend development. All code generated or written for the backend MUST strictly follow these standards.

## 🚨 MANDATORY CODE EXPLANATION RULE

**CRITICAL REQUIREMENT**: For every code change, snippet, or implementation, you MUST provide:

1. **Purpose Explanation**: What the code does and why it's needed
2. **Pattern Justification**: Why this specific pattern/approach was chosen
3. **Java 23 Features**: If using Java 23 features, explain the benefit
4. **Architecture Alignment**: How it fits the modular structure (api/domain/data/app)
5. **Standards Compliance**: Which coding standards it follows

### Example Format:
```java
// Code snippet here
```

**Explanation:**
- **Purpose**: [What this code accomplishes]
- **Pattern**: [Why this pattern was selected]
- **Java 23**: [Any Java 23 features used and their benefits]
- **Architecture**: [How it fits the modular structure]
- **Standards**: [Which standards it follows]

## 1. Project Structure

### Standard Maven/Gradle Structure
```
cafm-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cafm/backend/
│   │   │       ├── CafmBackendApplication.java
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── entity/
│   │   │       ├── dto/
│   │   │       ├── mapper/
│   │   │       ├── exception/
│   │   │       ├── security/
│   │   │       ├── util/
│   │   │       ├── constant/
│   │   │       ├── validation/
│   │   │       ├── filter/
│   │   │       ├── interceptor/
│   │   │       └── event/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── db/migration/
│   │       ├── static/
│   │       └── templates/
│   └── test/
│       └── java/
│           └── com/cafm/backend/
│               ├── unit/
│               ├── integration/
│               └── e2e/
├── pom.xml or build.gradle
├── Dockerfile
├── docker-compose.yml
└── README.md
```

### Package Naming Conventions
- **Base package**: `com.cafm.backend`
- **All lowercase**: No uppercase letters in package names
- **Singular names**: `entity`, `controller`, `service` (not entities, controllers)
- **Descriptive subpackages**: `com.cafm.backend.security.jwt`

## 2. Naming Conventions

### Classes
```java
// Entity classes - Singular, no suffix
@Entity
public class User { }

// Controllers - RestController suffix
@RestController
public class UserRestController { }

// Services - Service suffix for interface, ServiceImpl for implementation
public interface UserService { }

@Service
public class UserServiceImpl implements UserService { }

// Repositories - Repository suffix
@Repository
public interface UserRepository extends JpaRepository<User, Long> { }

// DTOs - Request/Response suffix
public class UserCreateRequest { }
public class UserResponse { }
public class UserUpdateRequest { }

// Mappers - Mapper suffix
@Mapper(componentModel = "spring")
public interface UserMapper { }

// Configurations - Config suffix
@Configuration
public class SecurityConfig { }

// Exceptions - Exception suffix
public class ResourceNotFoundException extends RuntimeException { }

// Utilities - Utils suffix
public class DateUtils { }

// Constants - Constants suffix or final class
public final class AppConstants { }

// Validators - Validator suffix
@Component
public class EmailValidator { }
```

### Methods
```java
// RESTful naming for controllers
@GetMapping
public ResponseEntity<List<UserResponse>> getAllUsers() { }

@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) { }

@PostMapping
public ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserCreateRequest request) { }

@PutMapping("/{id}")
public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody @Valid UserUpdateRequest request) { }

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) { }

// Service methods - business-oriented names
public User findUserByEmail(String email);
public User registerNewUser(UserRegistrationDto registration);
public void sendPasswordResetEmail(String email);
public boolean isEmailAvailable(String email);

// Repository methods - Spring Data naming convention
Optional<User> findByEmail(String email);
List<User> findByStatusAndCreatedAtAfter(UserStatus status, LocalDateTime date);
boolean existsByEmail(String email);
@Query("SELECT u FROM User u WHERE u.role = :role")
List<User> findUsersWithRole(@Param("role") Role role);
```

### Variables and Constants
```java
// Constants - UPPER_SNAKE_CASE
public static final String DEFAULT_PAGE_SIZE = "20";
public static final int MAX_LOGIN_ATTEMPTS = 3;
private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

// Variables - camelCase
private String userName;
private LocalDateTime createdAt;
private boolean isActive;

// Method parameters - camelCase
public User createUser(String firstName, String lastName, String email) { }
```

## 3. Entity/Model Standards

### JPA Entity Pattern
```java
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "reports"})
@EqualsAndHashCode(exclude = {"reports", "createdAt", "updatedAt"})
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Column(nullable = false, length = 50)
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @Column(nullable = false, length = 50)
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @Column(nullable = false)
    @JsonIgnore
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    private String modifiedBy;
    
    @Version
    private Long version;
    
    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Report> reports = new ArrayList<>();
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_schools",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "school_id")
    )
    private Set<School> schools = new HashSet<>();
    
    // Business methods
    public void addReport(Report report) {
        reports.add(report);
        report.setUser(this);
    }
    
    public void removeReport(Report report) {
        reports.remove(report);
        report.setUser(null);
    }
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
        if (role == null) {
            role = Role.USER;
        }
    }
}
```

### Enum Pattern
```java
public enum UserStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    SUSPENDED("suspended"),
    DELETED("deleted");
    
    private final String value;
    
    UserStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static UserStatus fromValue(String value) {
        for (UserStatus status : UserStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid UserStatus value: " + value);
    }
}
```

## 4. DTO (Data Transfer Object) Standards

### Request DTO Pattern
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must be at least 8 characters with uppercase, lowercase, number and special character"
    )
    private String password;
    
    @NotNull(message = "Role is required")
    private Role role;
    
    private List<Long> schoolIds;
}
```

### Response DTO Pattern
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private UserStatus status;
    private Role role;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private List<SchoolResponse> schools;
    private UserStatsResponse statistics;
    
    // Computed field
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

### Pagination DTO
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRequest {
    @Min(value = 0, message = "Page number cannot be negative")
    private int page = 0;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private int size = 20;
    
    private String sortBy = "id";
    
    @Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be ASC or DESC")
    private String sortDirection = "ASC";
    
    public org.springframework.data.domain.Pageable toPageable() {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
    
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent())
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .empty(page.isEmpty())
            .build();
    }
}
```

## 5. Controller Standards

### REST Controller Pattern
```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Management", description = "APIs for managing users")
@Slf4j
public class UserRestController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a paginated list of all users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status) {
        
        log.info("Fetching users: page={}, size={}, search={}, status={}", 
                pageRequest.getPage(), pageRequest.getSize(), search, status);
        
        Page<User> users = userService.findAllUsers(pageRequest.toPageable(), search, status);
        Page<UserResponse> userResponses = users.map(userMapper::toResponse);
        
        return ResponseEntity.ok(PageResponse.of(userResponses));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a single user by their ID")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable @Positive(message = "ID must be positive") Long id) {
        
        log.info("Fetching user with id: {}", id);
        User user = userService.findUserById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
    
    @PostMapping
    @Operation(summary = "Create new user", description = "Create a new user account")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @RequestBody @Valid UserCreateRequest request) {
        
        log.info("Creating new user with email: {}", request.getEmail());
        User user = userService.createUser(request);
        UserResponse response = userMapper.toResponse(user);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        
        return ResponseEntity.created(location).body(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable @Positive Long id,
            @RequestBody @Valid UserUpdateRequest request) {
        
        log.info("Updating user with id: {}", id);
        User user = userService.updateUser(id, request);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable @Positive Long id) {
        
        log.info("Deleting user with id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user status", description = "Change user account status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable @Positive Long id,
            @RequestParam @NotNull UserStatus status) {
        
        log.info("Updating user {} status to: {}", id, status);
        User user = userService.updateUserStatus(id, status);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
    
    @GetMapping("/{id}/reports")
    @Operation(summary = "Get user reports", description = "Get all reports created by a user")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<List<ReportResponse>> getUserReports(
            @PathVariable @Positive Long id) {
        
        log.info("Fetching reports for user: {}", id);
        List<Report> reports = userService.getUserReports(id);
        return ResponseEntity.ok(
            reports.stream()
                .map(reportMapper::toResponse)
                .collect(Collectors.toList())
        );
    }
}
```

## 6. Service Layer Standards

### Service Interface
```java
public interface UserService {
    
    // CRUD operations
    User createUser(UserCreateRequest request);
    User findUserById(Long id);
    Page<User> findAllUsers(Pageable pageable, String search, UserStatus status);
    User updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
    
    // Business operations
    User findUserByEmail(String email);
    boolean existsByEmail(String email);
    User updateUserStatus(Long id, UserStatus status);
    void resetPassword(String email);
    void changePassword(Long userId, ChangePasswordRequest request);
    List<Report> getUserReports(Long userId);
    UserStatsResponse getUserStatistics(Long userId);
    
    // Batch operations
    List<User> createUsersInBatch(List<UserCreateRequest> requests);
    void deleteUsersInBatch(List<Long> userIds);
    
    // Query operations
    List<User> findActiveUsersBySchool(Long schoolId);
    Page<User> searchUsers(UserSearchCriteria criteria, Pageable pageable);
}
```

### Service Implementation
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(UserCreateRequest request) {
        log.debug("Creating new user with email: {}", request.getEmail());
        
        // Validation
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }
        
        // Build entity
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Handle relationships
        if (request.getSchoolIds() != null && !request.getSchoolIds().isEmpty()) {
            Set<School> schools = new HashSet<>(schoolRepository.findAllById(request.getSchoolIds()));
            if (schools.size() != request.getSchoolIds().size()) {
                throw new ResourceNotFoundException("One or more schools not found");
            }
            user.setSchools(schools);
        }
        
        // Save
        User savedUser = userRepository.save(user);
        
        // Publish event
        eventPublisher.publishEvent(new UserCreatedEvent(this, savedUser));
        
        // Send welcome email
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
        
        log.info("User created successfully with id: {}", savedUser.getId());
        return savedUser;
    }
    
    @Override
    @Cacheable(value = "users", key = "#id")
    public User findUserById(Long id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
    
    @Override
    public Page<User> findAllUsers(Pageable pageable, String search, UserStatus status) {
        log.debug("Finding all users with search: {}, status: {}", search, status);
        
        Specification<User> spec = Specification.where(null);
        
        if (StringUtils.hasText(search)) {
            spec = spec.and(UserSpecifications.searchByKeyword(search));
        }
        
        if (status != null) {
            spec = spec.and(UserSpecifications.hasStatus(status));
        }
        
        return userRepository.findAll(spec, pageable);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User updateUser(Long id, UserUpdateRequest request) {
        log.debug("Updating user with id: {}", id);
        
        User user = findUserById(id);
        
        // Update fields
        userMapper.updateUserFromRequest(request, user);
        
        // Handle schools update if provided
        if (request.getSchoolIds() != null) {
            Set<School> schools = new HashSet<>(schoolRepository.findAllById(request.getSchoolIds()));
            user.setSchools(schools);
        }
        
        User updatedUser = userRepository.save(user);
        
        // Publish event
        eventPublisher.publishEvent(new UserUpdatedEvent(this, updatedUser));
        
        log.info("User updated successfully with id: {}", id);
        return updatedUser;
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteUser(Long id) {
        log.debug("Deleting user with id: {}", id);
        
        User user = findUserById(id);
        
        // Soft delete
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Or hard delete
        // userRepository.delete(user);
        
        // Publish event
        eventPublisher.publishEvent(new UserDeletedEvent(this, id));
        
        log.info("User deleted successfully with id: {}", id);
    }
    
    @Override
    public User findUserByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    @Override
    @Transactional
    public void resetPassword(String email) {
        log.debug("Resetting password for email: {}", email);
        
        User user = findUserByEmail(email);
        
        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        
        // Send reset email
        emailService.sendPasswordResetEmail(email, resetToken);
        
        log.info("Password reset email sent to: {}", email);
    }
    
    // Private helper methods
    private void validatePasswordStrength(String password) {
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new ValidationException("Password does not meet security requirements");
        }
    }
    
    private void sendNotification(User user, String message) {
        // Implementation
    }
}
```

## 7. Repository Standards

### JPA Repository Pattern
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long>, 
        JpaSpecificationExecutor<User>, UserRepositoryCustom {
    
    // Basic queries
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailAndStatus(String email, UserStatus status);
    
    boolean existsByEmail(String email);
    
    // Collection queries
    List<User> findByStatus(UserStatus status);
    
    List<User> findByRole(Role role);
    
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Pagination
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    
    // Custom queries with @Query
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u JOIN u.schools s WHERE s.id = :schoolId")
    List<User> findUsersBySchoolId(@Param("schoolId") Long schoolId);
    
    @Query(value = "SELECT * FROM users WHERE created_at > :date", nativeQuery = true)
    List<User> findRecentUsers(@Param("date") LocalDateTime date);
    
    // Modifying queries
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    void updateUserStatus(@Param("id") Long id, @Param("status") UserStatus status);
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.status = 'DELETED' AND u.deletedAt < :date")
    void deleteOldDeletedUsers(@Param("date") LocalDateTime date);
    
    // Projections
    @Query("SELECT new com.cafm.backend.dto.UserSummary(u.id, u.email, u.firstName, u.lastName) FROM User u")
    List<UserSummary> findAllUserSummaries();
    
    // Entity Graph
    @EntityGraph(attributePaths = {"schools", "reports"})
    Optional<User> findWithSchoolsAndReportsById(Long id);
}
```

### Custom Repository Implementation
```java
public interface UserRepositoryCustom {
    Page<User> searchUsers(UserSearchCriteria criteria, Pageable pageable);
    List<User> findUsersWithCustomLogic(Map<String, Object> params);
}

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {
    
    private final EntityManager entityManager;
    
    @Override
    public Page<User> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> root = query.from(User.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (StringUtils.hasText(criteria.getKeyword())) {
            Predicate emailPredicate = cb.like(root.get("email"), "%" + criteria.getKeyword() + "%");
            Predicate firstNamePredicate = cb.like(root.get("firstName"), "%" + criteria.getKeyword() + "%");
            Predicate lastNamePredicate = cb.like(root.get("lastName"), "%" + criteria.getKeyword() + "%");
            predicates.add(cb.or(emailPredicate, firstNamePredicate, lastNamePredicate));
        }
        
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
        
        if (criteria.getRole() != null) {
            predicates.add(cb.equal(root.get("role"), criteria.getRole()));
        }
        
        if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
            predicates.add(cb.between(root.get("createdAt"), criteria.getStartDate(), criteria.getEndDate()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(sort -> {
                if (sort.isAscending()) {
                    orders.add(cb.asc(root.get(sort.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(sort.getProperty())));
                }
            });
            query.orderBy(orders);
        }
        
        TypedQuery<User> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<User> results = typedQuery.getResultList();
        
        // Get total count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        countQuery.select(cb.count(countQuery.from(User.class)));
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    public List<User> findUsersWithCustomLogic(Map<String, Object> params) {
        // Complex custom implementation
        return new ArrayList<>();
    }
}
```

### Specification Pattern
```java
public class UserSpecifications {
    
    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) -> {
            if (email == null) return cb.conjunction();
            return cb.equal(root.get("email"), email);
        };
    }
    
    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }
    
    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> {
            if (role == null) return cb.conjunction();
            return cb.equal(root.get("role"), role);
        };
    }
    
    public static Specification<User> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null || end == null) return cb.conjunction();
            return cb.between(root.get("createdAt"), start, end);
        };
    }
    
    public static Specification<User> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            
            String pattern = "%" + keyword.toLowerCase() + "%";
            
            Predicate emailPredicate = cb.like(cb.lower(root.get("email")), pattern);
            Predicate firstNamePredicate = cb.like(cb.lower(root.get("firstName")), pattern);
            Predicate lastNamePredicate = cb.like(cb.lower(root.get("lastName")), pattern);
            
            return cb.or(emailPredicate, firstNamePredicate, lastNamePredicate);
        };
    }
    
    public static Specification<User> belongsToSchool(Long schoolId) {
        return (root, query, cb) -> {
            if (schoolId == null) return cb.conjunction();
            
            Join<User, School> schoolJoin = root.join("schools", JoinType.LEFT);
            return cb.equal(schoolJoin.get("id"), schoolId);
        };
    }
}
```

## 8. Exception Handling Standards

### Custom Exception Classes
```java
// Base exception
@Getter
public abstract class BaseException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final LocalDateTime timestamp;
    
    protected BaseException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();
    }
    
    protected BaseException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();
    }
}

// Specific exceptions
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

public class DuplicateResourceException extends BaseException {
    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE", HttpStatus.CONFLICT);
    }
}

public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }
}

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }
}

public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }
}

public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(message, "BUSINESS_ERROR", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
```

### Global Exception Handler
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .path(getPath())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.error("Duplicate resource: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .message(ex.getMessage())
            .path(getPath())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Invalid input parameters")
            .validationErrors(errors)
            .path(getPath())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        
        String message = "Database constraint violation";
        if (ex.getCause() instanceof ConstraintViolationException) {
            ConstraintViolationException constraintEx = (ConstraintViolationException) ex.getCause();
            if (constraintEx.getConstraintName() != null) {
                message = "Constraint violation: " + constraintEx.getConstraintName();
            }
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Data Integrity Violation")
            .message(message)
            .path(getPath())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error(HttpStatus.FORBIDDEN.getReasonPhrase())
            .message("Access denied")
            .path(getPath())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message("An unexpected error occurred")
            .path(getPath())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private String getPath() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();
        return request.getRequestURI();
    }
}
```

### Error Response DTO
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
    private String traceId;
    
    public ErrorResponse(String message) {
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }
}
```

## 9. Configuration Standards

### Application Configuration
```yaml
# application.yml
spring:
  application:
    name: cafm-backend
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/cafm_db}
    username: ${DB_USERNAME:cafm_user}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    hikari:
      pool-name: CafmHikariPool
      maximum-pool-size: ${DB_MAX_POOL_SIZE:10}
      minimum-idle: ${DB_MIN_IDLE:5}
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
  
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:validate}
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
    show-sql: ${JPA_SHOW_SQL:false}
    open-in-view: false
  
  liquibase:
    enabled: ${LIQUIBASE_ENABLED:true}
    change-log: classpath:db/changelog/db.changelog-master.xml
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
  
  cache:
    type: redis
    redis:
      time-to-live: ${CACHE_TTL:3600000}
      cache-null-values: false
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8080}
          jwk-set-uri: ${JWT_JWK_SET_URI:http://localhost:8080/.well-known/jwks.json}

server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /api
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
  error:
    include-message: always
    include-binding-errors: always
    include-stacktrace: on_param

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com.cafm.backend: ${APP_LOG_LEVEL:DEBUG}
    org.springframework.web: INFO
    org.hibernate.SQL: ${SQL_LOG_LEVEL:INFO}
    org.hibernate.type.descriptor.sql.BasicBinder: ${SQL_LOG_LEVEL:INFO}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30

app:
  jwt:
    secret: ${JWT_SECRET:your-secret-key}
    expiration: ${JWT_EXPIRATION:86400000}
    refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}
  
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:4200}
    allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
    allowed-headers: "*"
    allow-credentials: true
  
  pagination:
    default-page-size: 20
    max-page-size: 100
  
  file-upload:
    max-size: ${MAX_FILE_SIZE:10MB}
    allowed-extensions: jpg,jpeg,png,pdf,doc,docx,xls,xlsx
  
  rate-limit:
    enabled: ${RATE_LIMIT_ENABLED:true}
    requests-per-minute: ${RATE_LIMIT_RPM:60}
```

### Configuration Classes
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/api/v1/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", HttpServletResponse.SC_FORBIDDEN);
            body.put("error", "Forbidden");
            body.put("message", accessDeniedException.getMessage());
            body.put("path", request.getServletPath());
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), body);
        };
    }
}
```

## 10. Mapper Standards (MapStruct)

### Mapper Interface
```java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface UserMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toEntity(UserCreateRequest request);
    
    @Mapping(target = "fullName", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    @Mapping(target = "schools", source = "schools")
    UserResponse toResponse(User user);
    
    List<UserResponse> toResponseList(List<User> users);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);
    
    @AfterMapping
    default void linkSchools(@MappingTarget User user) {
        if (user.getReports() != null) {
            user.getReports().forEach(report -> report.setUser(user));
        }
    }
    
    default SchoolResponse schoolToSchoolResponse(School school) {
        if (school == null) return null;
        
        return SchoolResponse.builder()
            .id(school.getId())
            .name(school.getName())
            .address(school.getAddress())
            .build();
    }
}
```

## 11. Testing Standards

### Unit Test Pattern
```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SchoolRepository schoolRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private User testUser;
    private UserCreateRequest createRequest;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .status(UserStatus.ACTIVE)
            .role(Role.USER)
            .build();
        
        createRequest = UserCreateRequest.builder()
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .password("Password123!")
            .role(Role.USER)
            .build();
    }
    
    @Test
    @DisplayName("Should create user successfully")
    void createUser_WithValidData_ShouldCreateSuccessfully() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(UserCreateRequest.class))).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        User result = userService.createUser(createRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when email already exists")
    void createUser_WithExistingEmail_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        // When & Then
        assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(createRequest);
        });
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should find user by id successfully")
    void findUserById_WithValidId_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        User result = userService.findUserById(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }
    
    @Test
    @DisplayName("Should throw exception when user not found")
    void findUserById_WithInvalidId_ShouldThrowException() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.findUserById(999L)
        );
        
        assertThat(exception.getMessage()).contains("User not found with id: 999");
        verify(userRepository).findById(999L);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"test@example.com", "admin@example.com", "user@example.com"})
    @DisplayName("Should validate email existence")
    void existsByEmail_WithVariousEmails_ShouldReturnCorrectResult(String email) {
        // Given
        when(userRepository.existsByEmail(email)).thenReturn(true);
        
        // When
        boolean exists = userService.existsByEmail(email);
        
        // Then
        assertThat(exists).isTrue();
        verify(userRepository).existsByEmail(email);
    }
}
```

### Integration Test Pattern
```java
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @MockBean
    private EmailService emailService;
    
    private String authToken;
    
    @BeforeEach
    void setUp() throws Exception {
        authToken = obtainAuthToken("admin@example.com", "password");
    }
    
    @Test
    @DisplayName("Should get all users with pagination")
    void getAllUsers_WithPagination_ShouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + authToken)
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("sortDirection", "DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(10))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.totalElements").exists())
            .andDo(print());
    }
    
    @Test
    @DisplayName("Should create user successfully")
    @Transactional
    @Rollback
    void createUser_WithValidData_ShouldReturnCreatedUser() throws Exception {
        UserCreateRequest request = UserCreateRequest.builder()
            .email("newuser@example.com")
            .firstName("New")
            .lastName("User")
            .password("Password123!")
            .role(Role.USER)
            .build();
        
        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.firstName").value("New"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.id").exists())
            .andDo(print());
        
        // Verify database
        assertTrue(userRepository.existsByEmail("newuser@example.com"));
    }
    
    @Test
    @DisplayName("Should return 404 when user not found")
    void getUserById_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/users/9999")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found with id: 9999"))
            .andDo(print());
    }
    
    @Test
    @DisplayName("Should return 401 when not authenticated")
    void getAllUsers_WithoutAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized())
            .andDo(print());
    }
    
    private String obtainAuthToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("accessToken").asText();
    }
}
```

## 12. Security Standards

### JWT Configuration
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;
    
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername(), jwtExpiration);
    }
    
    public String generateRefreshToken(UserDetails userDetails) {
        return createToken(new HashMap<>(), userDetails.getUsername(), refreshExpiration);
    }
    
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    
    private boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### Authentication Filter
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtTokenProvider.getUsernameFromToken(token);
            } catch (IllegalArgumentException e) {
                log.error("Unable to get JWT Token");
            } catch (ExpiredJwtException e) {
                log.error("JWT Token has expired");
            } catch (MalformedJwtException e) {
                log.error("Invalid JWT Token");
            }
        }
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtTokenProvider.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

## 13. Validation Standards

### Custom Validators
```java
// Annotation
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Documented
public @interface ValidPhoneNumber {
    String message() default "Invalid phone number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
@Component
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    private static final String PHONE_PATTERN = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[0-9]{1,9}$";
    
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        // Initialization if needed
    }
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return true; // Let @NotNull handle null checks
        }
        return phoneNumber.matches(PHONE_PATTERN);
    }
}
```

### Validation Groups
```java
public interface ValidationGroups {
    interface Create {}
    interface Update {}
    interface Delete {}
}

@Data
public class UserRequest {
    
    @Null(groups = ValidationGroups.Create.class)
    @NotNull(groups = ValidationGroups.Update.class)
    private Long id;
    
    @NotBlank(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Email
    private String email;
    
    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(min = 8, groups = ValidationGroups.Create.class)
    private String password;
}
```

## 14. Database Migration Standards (Liquibase/Flyway)

### Flyway Migration Naming
```
V1__Initial_schema.sql
V2__Add_user_table.sql
V3__Add_indexes_to_user_table.sql
V4__Insert_default_roles.sql
U3__Undo_add_indexes.sql  // Undo migration
R__Repeatable_views.sql    // Repeatable migration
```

### Migration Script Example
```sql
-- V1__Initial_schema.sql
CREATE SCHEMA IF NOT EXISTS cafm;

CREATE TABLE cafm.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_users_email ON cafm.users(email);
CREATE INDEX idx_users_status ON cafm.users(status);
CREATE INDEX idx_users_created_at ON cafm.users(created_at DESC);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE
    ON cafm.users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

## 15. Logging Standards

### Logging Best Practices
```java
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    
    // Use parameterized messages
    public User createUser(UserCreateRequest request) {
        log.debug("Creating user with email: {}", request.getEmail());
        
        try {
            User user = // ... create user
            log.info("User created successfully with id: {} and email: {}", 
                    user.getId(), user.getEmail());
            return user;
        } catch (Exception e) {
            log.error("Failed to create user with email: {}", request.getEmail(), e);
            throw e;
        }
    }
    
    // Log levels:
    // TRACE - Detailed trace messages (rarely used)
    // DEBUG - Debug messages for development
    // INFO  - Informational messages (business events)
    // WARN  - Warning messages (recoverable issues)
    // ERROR - Error messages (exceptions, failures)
    
    // Examples:
    private void examples() {
        log.trace("Entering method with params: {}", params);
        log.debug("Calculated value: {}", calculatedValue);
        log.info("User {} logged in successfully", username);
        log.warn("API rate limit approaching for user: {}", userId);
        log.error("Database connection failed", exception);
    }
}
```

## 16. API Documentation (OpenAPI/Swagger)

### Swagger Configuration
```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "CAFM Backend API",
        version = "1.0",
        description = "School Maintenance Management System API",
        contact = @Contact(
            name = "CAFM Team",
            email = "support@cafm.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "http://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local server"),
        @Server(url = "https://api.cafm.com", description = "Production server")
    }
)
public class SwaggerConfig {
    
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("/api/v1/public/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/v1/admin/**")
            .addOpenApiCustomiser(openApi -> openApi.info(new Info()
                .title("Admin API")
                .version("1.0")))
            .build();
    }
}
```

## 17. Performance Optimization Standards

### Caching
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(60))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}

// Usage
@Cacheable(value = "users", key = "#id")
public User findUserById(Long id) { }

@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) { }

@CachePut(value = "users", key = "#user.id")
public User updateUser(User user) { }
```

### Database Query Optimization
```java
// Use projections for read-only data
public interface UserProjection {
    Long getId();
    String getEmail();
    String getFullName();
}

// Use @EntityGraph to avoid N+1 queries
@EntityGraph(attributePaths = {"schools", "reports"})
Optional<User> findWithDetailsById(Long id);

// Use batch processing
@Modifying
@Query("UPDATE User u SET u.status = :status WHERE u.id IN :ids")
void updateStatusInBatch(@Param("ids") List<Long> ids, @Param("status") UserStatus status);
```

## 18. Code Quality Standards

### General Rules
1. **DRY (Don't Repeat Yourself)**: Avoid code duplication
2. **KISS (Keep It Simple, Stupid)**: Write simple, readable code
3. **YAGNI (You Aren't Gonna Need It)**: Don't add functionality until needed
4. **SOLID Principles**: Follow all SOLID principles
5. **Clean Code**: Follow Robert C. Martin's Clean Code principles

### Method Guidelines
- Methods should do one thing
- Maximum 20 lines per method (prefer smaller)
- Maximum 3 parameters (use objects for more)
- Avoid nested loops and deep nesting
- Use early returns to reduce nesting

### Class Guidelines
- Single Responsibility Principle
- Maximum 300 lines per class
- Cohesive methods and properties
- Proper encapsulation

### Comments and Documentation
- Write self-documenting code
- Use JavaDoc for public APIs
- Avoid obvious comments
- Explain "why" not "what"
- Keep comments up-to-date

## 19. Git Commit Standards

### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes
- **refactor**: Code refactoring
- **perf**: Performance improvements
- **test**: Test additions or fixes
- **chore**: Build process or auxiliary tool changes

### Examples
```
feat(user): add email verification functionality

- Implement email verification service
- Add verification token to user entity
- Create verification endpoint
- Add email templates

Closes #123
```

## 20. Code Review Checklist

Before submitting code:
- [ ] Code follows naming conventions
- [ ] Proper exception handling
- [ ] Unit tests written (minimum 80% coverage)
- [ ] Integration tests for APIs
- [ ] No hardcoded values
- [ ] Proper logging added
- [ ] Documentation updated
- [ ] Security best practices followed
- [ ] Performance considerations addressed
- [ ] Database migrations included
- [ ] API documentation updated
- [ ] No code smells or violations
- [ ] Passed SonarQube analysis
- [ ] Reviewed for OWASP Top 10

## Enforcement

These standards are mandatory for all Spring Boot backend code. Use tools like:
- **Checkstyle**: Code style enforcement
- **SpotBugs**: Bug detection
- **PMD**: Code quality rules
- **SonarQube**: Continuous code quality
- **JaCoCo**: Code coverage
- **Spring Boot DevTools**: Development productivity

All code must pass quality gates before merging.