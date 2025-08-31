# Variables for CAFM Backend Infrastructure
# This file defines all configurable parameters for the infrastructure

# ==============================================================================
# GENERAL CONFIGURATION
# ==============================================================================
variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
  validation {
    condition = contains(["dev", "staging", "production"], var.environment)
    error_message = "Environment must be one of: dev, staging, production."
  }
}

variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-west-2"
}

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "cafm-backend"
}

# ==============================================================================
# NETWORKING CONFIGURATION
# ==============================================================================
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
  
  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "VPC CIDR must be a valid IPv4 CIDR block."
  }
}

variable "allowed_origins" {
  description = "Allowed origins for CORS configuration"
  type        = list(string)
  default     = []
}

# ==============================================================================
# EKS CLUSTER CONFIGURATION
# ==============================================================================
variable "kubernetes_version" {
  description = "Kubernetes version for EKS cluster"
  type        = string
  default     = "1.29"
}

variable "eks_node_instance_types" {
  description = "Instance types for EKS node group"
  type        = list(string)
  default     = ["t3.medium"]
}

variable "eks_node_min_size" {
  description = "Minimum number of nodes in EKS node group"
  type        = number
  default     = 1
  
  validation {
    condition     = var.eks_node_min_size >= 1
    error_message = "Minimum node size must be at least 1."
  }
}

variable "eks_node_max_size" {
  description = "Maximum number of nodes in EKS node group"
  type        = number
  default     = 10
  
  validation {
    condition     = var.eks_node_max_size >= var.eks_node_min_size
    error_message = "Maximum node size must be greater than or equal to minimum node size."
  }
}

variable "eks_node_desired_size" {
  description = "Desired number of nodes in EKS node group"
  type        = number
  default     = 3
  
  validation {
    condition = (
      var.eks_node_desired_size >= var.eks_node_min_size &&
      var.eks_node_desired_size <= var.eks_node_max_size
    )
    error_message = "Desired node size must be between minimum and maximum node sizes."
  }
}

# ==============================================================================
# DATABASE CONFIGURATION
# ==============================================================================
variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "rds_allocated_storage" {
  description = "Initial allocated storage for RDS instance (GB)"
  type        = number
  default     = 20
  
  validation {
    condition     = var.rds_allocated_storage >= 20
    error_message = "Allocated storage must be at least 20 GB."
  }
}

variable "rds_max_allocated_storage" {
  description = "Maximum allocated storage for RDS instance (GB)"
  type        = number
  default     = 100
  
  validation {
    condition     = var.rds_max_allocated_storage >= var.rds_allocated_storage
    error_message = "Maximum allocated storage must be greater than or equal to allocated storage."
  }
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "cafm_db"
  
  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9_]*$", var.db_name))
    error_message = "Database name must start with a letter and contain only alphanumeric characters and underscores."
  }
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "cafm_user"
  
  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9_]*$", var.db_username))
    error_message = "Database username must start with a letter and contain only alphanumeric characters and underscores."
  }
}

# ==============================================================================
# REDIS CONFIGURATION
# ==============================================================================
variable "redis_node_type" {
  description = "ElastiCache node type for Redis"
  type        = string
  default     = "cache.t3.micro"
}

# ==============================================================================
# MONITORING CONFIGURATION
# ==============================================================================
variable "alert_email" {
  description = "Email address for CloudWatch alerts"
  type        = string
  default     = ""
  
  validation {
    condition = (
      var.alert_email == "" ||
      can(regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", var.alert_email))
    )
    error_message = "Alert email must be a valid email address or empty string."
  }
}

# ==============================================================================
# SECURITY CONFIGURATION
# ==============================================================================
variable "enable_vpc_flow_logs" {
  description = "Enable VPC Flow Logs"
  type        = bool
  default     = true
}

variable "enable_guardduty" {
  description = "Enable AWS GuardDuty"
  type        = bool
  default     = false
}

variable "enable_security_hub" {
  description = "Enable AWS Security Hub"
  type        = bool
  default     = false
}

# ==============================================================================
# BACKUP CONFIGURATION
# ==============================================================================
variable "backup_retention_days" {
  description = "Number of days to retain backups"
  type        = number
  default     = 7
  
  validation {
    condition     = var.backup_retention_days >= 0 && var.backup_retention_days <= 35
    error_message = "Backup retention days must be between 0 and 35."
  }
}

# ==============================================================================
# FEATURE FLAGS
# ==============================================================================
variable "enable_nat_gateway" {
  description = "Enable NAT Gateway for private subnets"
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Use a single NAT Gateway for all private subnets (cost optimization for non-production)"
  type        = bool
  default     = false
}

variable "enable_container_insights" {
  description = "Enable Container Insights for EKS cluster"
  type        = bool
  default     = true
}

variable "enable_irsa" {
  description = "Enable IAM Roles for Service Accounts (IRSA)"
  type        = bool
  default     = true
}

# ==============================================================================
# COST OPTIMIZATION
# ==============================================================================
variable "use_spot_instances" {
  description = "Use Spot instances for non-critical workloads"
  type        = bool
  default     = false
}

variable "enable_storage_lifecycle" {
  description = "Enable S3 storage lifecycle policies"
  type        = bool
  default     = true
}

# ==============================================================================
# COMPLIANCE AND GOVERNANCE
# ==============================================================================
variable "enable_config" {
  description = "Enable AWS Config for compliance monitoring"
  type        = bool
  default     = false
}

variable "enable_cloudtrail" {
  description = "Enable CloudTrail for API logging"
  type        = bool
  default     = true
}

variable "encryption_at_rest" {
  description = "Enable encryption at rest for all applicable services"
  type        = bool
  default     = true
}

variable "encryption_in_transit" {
  description = "Enable encryption in transit for all applicable services"
  type        = bool
  default     = true
}

# ==============================================================================
# DISASTER RECOVERY
# ==============================================================================
variable "multi_az_deployment" {
  description = "Enable Multi-AZ deployment for high availability"
  type        = bool
  default     = false
}

variable "cross_region_backup" {
  description = "Enable cross-region backup replication"
  type        = bool
  default     = false
}

variable "backup_schedule" {
  description = "Cron expression for backup schedule"
  type        = string
  default     = "cron(0 2 * * ? *)" # Daily at 2 AM UTC
  
  validation {
    condition     = can(regex("^cron\\(", var.backup_schedule))
    error_message = "Backup schedule must be a valid cron expression starting with 'cron('."
  }
}

# ==============================================================================
# PERFORMANCE TUNING
# ==============================================================================
variable "enable_performance_insights" {
  description = "Enable Performance Insights for RDS"
  type        = bool
  default     = false
}

variable "enhanced_monitoring" {
  description = "Enable enhanced monitoring for RDS"
  type        = bool
  default     = false
}

# ==============================================================================
# ENVIRONMENT-SPECIFIC DEFAULTS
# ==============================================================================
locals {
  # Environment-specific default overrides
  env_defaults = {
    dev = {
      eks_node_min_size       = 1
      eks_node_max_size       = 3
      eks_node_desired_size   = 2
      rds_instance_class      = "db.t3.micro"
      redis_node_type         = "cache.t3.micro"
      single_nat_gateway      = true
      multi_az_deployment     = false
      backup_retention_days   = 3
      enable_performance_insights = false
      enhanced_monitoring     = false
    }
    
    staging = {
      eks_node_min_size       = 2
      eks_node_max_size       = 5
      eks_node_desired_size   = 3
      rds_instance_class      = "db.t3.small"
      redis_node_type         = "cache.t3.small"
      single_nat_gateway      = false
      multi_az_deployment     = true
      backup_retention_days   = 7
      enable_performance_insights = true
      enhanced_monitoring     = true
    }
    
    production = {
      eks_node_min_size       = 3
      eks_node_max_size       = 20
      eks_node_desired_size   = 5
      rds_instance_class      = "db.r5.large"
      redis_node_type         = "cache.r5.large"
      single_nat_gateway      = false
      multi_az_deployment     = true
      backup_retention_days   = 30
      enable_performance_insights = true
      enhanced_monitoring     = true
      cross_region_backup     = true
      enable_guardduty        = true
      enable_security_hub     = true
      enable_config          = true
    }
  }
}

# ==============================================================================
# TAGS
# ==============================================================================
variable "additional_tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
}