package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing FCM (Firebase Cloud Messaging) device tokens.
 * Manages device tokens for push notifications across different platforms.
 */
@Entity
@Table(name = "user_fcm_tokens", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"fcm_token"})
       })
public class FCMToken extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "fcm_token", nullable = false, unique = true)
    private String token;

    @NotNull
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public FCMToken() {
        super();
    }

    private FCMToken(Builder builder) {
        this.user = builder.user;
        this.token = builder.token;
        this.platform = builder.platform;
        this.deviceId = builder.deviceId;
        this.active = builder.active;
        this.company = builder.company;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }


    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private User user;
        private String token;
        private String platform;
        private String deviceId;
        private Boolean active = true;
        private Company company;

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }


        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }


        public Builder company(Company company) {
            this.company = company;
            return this;
        }

        public FCMToken build() {
            return new FCMToken(this);
        }
    }

}