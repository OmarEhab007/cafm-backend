# PostgreSQL Enum UserType Implementation

This package contains the solution for handling PostgreSQL enum types in Hibernate without using `@ColumnTransformer` annotations that can cause transaction commit failures.

## Problem

The original implementation used `@ColumnTransformer` annotations like this:

```java
@org.hibernate.annotations.ColumnTransformer(write = "?::user_type")
@org.hibernate.annotations.ColumnTransformer(write = "?::user_status")
```

These annotations were causing transaction commit failures when working with PostgreSQL enum types.

## Solution

We created custom Hibernate UserType implementations that properly handle PostgreSQL enum types:

### 1. Generic Base Class

`PostgreSQLEnumUserType<E>` - A generic abstract base class that provides the foundation for handling any PostgreSQL enum type with Hibernate 6.x.

**Key Features:**
- Proper handling of PostgreSQL `PGobject` types
- Type-safe enum conversion
- Error handling for invalid database values
- Immutable enum support (proper deep copy, equals, hashCode)

### 2. Specific Implementations

- `UserTypeUserType` - Handles the `UserType` enum with PostgreSQL `user_type` enum
- `UserStatusUserType` - Handles the `UserStatus` enum with PostgreSQL `user_status` enum

## Usage

### Before (Problematic)

```java
@Column(name = "user_type", nullable = false, columnDefinition = "VARCHAR(30)")
@Convert(converter = UserTypeConverter.class)
@org.hibernate.annotations.ColumnTransformer(write = "?::user_type")
private UserType userType;
```

### After (Fixed)

```java
@Column(name = "user_type", nullable = false)
@org.hibernate.annotations.Type(UserTypeUserType.class)
private UserType userType;
```

## Technical Details

### How It Works

1. **Database Writing**: Converts Java enum values to PostgreSQL `PGobject` with proper type information
2. **Database Reading**: Handles both `PGobject` and `String` values from the database
3. **Type Safety**: Provides proper error handling for invalid database values
4. **Performance**: Optimized for immutable enum types with proper caching

### Database Types Supported

- `user_type` enum: `VIEWER`, `TECHNICIAN`, `SUPERVISOR`, `ADMIN`, `SUPER_ADMIN`
- `user_status` enum: `PENDING_VERIFICATION`, `ACTIVE`, `INACTIVE`, `SUSPENDED`, `LOCKED`, `ARCHIVED`

## Benefits

1. **No Transaction Failures**: Eliminates the commit issues caused by `@ColumnTransformer`
2. **Type Safety**: Proper Java enum to PostgreSQL enum conversion
3. **Error Handling**: Clear error messages for invalid database values
4. **Performance**: Efficient handling of immutable enum types
5. **Maintainability**: Clean, testable code without complex transformers

## Testing

The implementation is thoroughly tested with:

- **Unit Tests**: `PostgreSQLEnumUserTypeFunctionalTest` - 14 comprehensive test cases
- **Integration Tests**: `PostgreSQLEnumIntegrationTest` - Full database interaction tests
- **All enum values tested**: Ensures all enum variants work correctly
- **Error scenarios covered**: Tests for invalid values and edge cases

## Migration Notes

When migrating from the old `@ColumnTransformer` approach:

1. Remove `@ColumnTransformer` annotations
2. Remove `columnDefinition` from `@Column` annotations
3. Replace `@Convert(converter = ...)` with `@org.hibernate.annotations.Type(CustomUserType.class)`
4. Ensure PostgreSQL enum types exist in the database
5. Test thoroughly with actual database transactions

## Requirements

- Hibernate 6.x
- PostgreSQL with enum types created
- Java 23 (uses modern language features)
- Spring Boot 3.3.x

## Performance Considerations

- Enums are immutable, so deep copy operations are optimized
- PGobject creation is minimal and efficient
- Type conversion is cached at the enum level
- No reflection or complex type introspection required