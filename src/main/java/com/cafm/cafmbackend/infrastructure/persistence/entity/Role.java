package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Role entity for role-based access control.
 * 
 * Architecture: Part of RBAC system, linked to users via many-to-many relationship
 * Standards: NO Lombok as per requirements
 */
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {
    
    @Column(name = "name", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name cannot exceed 50 characters")
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
    
    // Constructors
    public Role() {
        super();
    }
    
    public Role(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Set<User> getUsers() {
        return users;
    }
    
    public void setUsers(Set<User> users) {
        this.users = users;
    }
    
    // toString, equals, hashCode
    @Override
    public String toString() {
        return String.format("Role[id=%s, name=%s]", getId(), name);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(name, role.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}