# Contributing to CAFM Backend

First off, thank you for considering contributing to CAFM Backend! It's people like you that make CAFM Backend such a great tool.

## Code of Conduct

This project and everyone participating in it is governed by the [CAFM Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [support@cafm.sa](mailto:support@cafm.sa).

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

- **Use a clear and descriptive title** for the issue to identify the problem
- **Describe the exact steps which reproduce the problem** in as many details as possible
- **Provide specific examples to demonstrate the steps**
- **Describe the behavior you observed after following the steps**
- **Explain which behavior you expected to see instead and why**
- **Include screenshots and animated GIFs** if possible
- **Include your environment details** (OS, Java version, etc.)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

- **Use a clear and descriptive title** for the issue to identify the suggestion
- **Provide a step-by-step description of the suggested enhancement**
- **Provide specific examples to demonstrate the steps**
- **Describe the current behavior** and **explain which behavior you expected to see instead**
- **Explain why this enhancement would be useful**

### Pull Requests

1. Fork the repo and create your branch from `main`
2. If you've added code that should be tested, add tests
3. If you've changed APIs, update the documentation
4. Ensure the test suite passes
5. Make sure your code follows the existing code style
6. Issue that pull request!

## Development Process

### Setting Up Your Development Environment

1. **Fork and Clone**
   ```bash
   git clone https://github.com/your-username/cafm-backend.git
   cd cafm-backend
   ```

2. **Set Up Infrastructure**
   ```bash
   docker compose up -d
   ```

3. **Install Dependencies**
   ```bash
   mvn clean install
   ```

4. **Run Tests**
   ```bash
   mvn test
   ```

### Code Style Guidelines

#### Java Code Style

- **Java Version**: Use Java 23 features where appropriate
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Maximum 120 characters
- **Naming Conventions**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

#### Best Practices

1. **Dependency Injection**: Use constructor injection only
   ```java
   // Good
   private final UserService userService;
   
   public UserController(UserService userService) {
       this.userService = userService;
   }
   
   // Bad
   @Autowired
   private UserService userService;
   ```

2. **DTOs**: Use Java records for DTOs
   ```java
   public record UserCreateRequest(
       @NotBlank String email,
       @NotBlank String firstName,
       @NotBlank String lastName
   ) {}
   ```

3. **Validation**: Always validate input
   ```java
   @PostMapping
   public ResponseEntity<UserResponse> createUser(
       @Valid @RequestBody UserCreateRequest request
   ) {
       // Implementation
   }
   ```

4. **Error Handling**: Use custom exceptions
   ```java
   throw new ResourceNotFoundException("User", "id", userId);
   ```

5. **Testing**: Write comprehensive tests
   ```java
   @Test
   @DisplayName("Should create user successfully")
   void shouldCreateUserSuccessfully() {
       // Given
       // When
       // Then
   }
   ```

### Testing Requirements

- **Minimum Coverage**: 80% for new code
- **Test Types Required**:
  - Unit tests for services and utilities
  - Integration tests for controllers
  - Repository tests with @DataJpaTest
- **Test Naming**: Use descriptive names with @DisplayName
- **Test Data**: Use test fixtures and builders

### Database Migrations

1. **Naming Convention**: `V{version}__{description}.sql`
   - Version: Sequential number (e.g., 127, 128, 129)
   - Description: Snake_case description

2. **Migration Rules**:
   - Never modify existing migrations
   - Always test migrations locally first
   - Include rollback considerations
   - Document breaking changes

3. **Example Migration**:
   ```sql
   -- V127__Add_user_preferences_table.sql
   CREATE TABLE user_preferences (
       id BIGSERIAL PRIMARY KEY,
       user_id BIGINT NOT NULL REFERENCES users(id),
       theme VARCHAR(20) DEFAULT 'light',
       language VARCHAR(10) DEFAULT 'en',
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   
   CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
   ```

### Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Types

- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that do not affect the meaning of the code
- **refactor**: A code change that neither fixes a bug nor adds a feature
- **perf**: A code change that improves performance
- **test**: Adding missing tests or correcting existing tests
- **build**: Changes that affect the build system or external dependencies
- **ci**: Changes to our CI configuration files and scripts
- **chore**: Other changes that don't modify src or test files

#### Examples

```
feat(auth): add two-factor authentication

Implemented TOTP-based 2FA for enhanced security.
Users can now enable 2FA from their profile settings.

Closes #123
```

```
fix(work-order): resolve N+1 query issue in listing

Added @EntityGraph to eagerly fetch relationships,
reducing database queries from 100+ to 2.

Performance improvement: ~80% faster response time
```

### Pull Request Process

1. **Branch Naming**: `feature/description` or `fix/description`
2. **PR Title**: Follow commit message format
3. **PR Description**: Use the PR template
4. **Required Checks**:
   - All tests passing
   - Code coverage maintained
   - No security vulnerabilities
   - Code review approval

### Code Review Guidelines

#### For Authors

- Keep PRs small and focused
- Provide context in the description
- Respond to feedback constructively
- Update based on reviews promptly

#### For Reviewers

- Be constructive and specific
- Approve promptly when satisfied
- Use suggestions for small changes
- Focus on:
  - Correctness
  - Performance
  - Security
  - Maintainability
  - Test coverage

## Documentation

### API Documentation

- Use OpenAPI annotations on all endpoints
- Include request/response examples
- Document error responses
- Keep Swagger descriptions updated

### Code Documentation

- JavaDoc for all public methods
- Explain complex algorithms
- Document business logic decisions
- Include examples where helpful

## Release Process

1. **Version Numbering**: Semantic Versioning (MAJOR.MINOR.PATCH)
2. **Release Notes**: Document all changes
3. **Testing**: Full regression testing
4. **Deployment**: Follow deployment checklist

## Getting Help

- **Discord**: [Join our Discord](https://discord.gg/cafm)
- **Email**: support@cafm.sa
- **Documentation**: [docs.cafm.sa](https://docs.cafm.sa)
- **Issues**: [GitHub Issues](https://github.com/OmarEhab007/cafm-backend/issues)

## Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- Project documentation

## License

By contributing, you agree that your contributions will be licensed under the MIT License.