# Staging Environment Configuration
# This file contains staging-specific variable values

# ==============================================================================
# GENERAL CONFIGURATION
# ==============================================================================
environment = "staging"
aws_region  = "us-west-2"

# ==============================================================================
# NETWORKING
# ==============================================================================
vpc_cidr = "10.1.0.0/16"

allowed_origins = [
  "https://staging.cafm.company.com",
  "https://staging-app.cafm.company.com",
  "https://staging-admin.cafm.company.com"
]

# ==============================================================================
# EKS CLUSTER CONFIGURATION
# ==============================================================================
kubernetes_version = "1.29"

eks_node_instance_types = ["t3.medium", "t3.large"]
eks_node_min_size       = 2
eks_node_max_size       = 8
eks_node_desired_size   = 3

# ==============================================================================
# DATABASE CONFIGURATION
# ==============================================================================
rds_instance_class      = "db.t3.small"
rds_allocated_storage   = 50
rds_max_allocated_storage = 200

db_name     = "cafm_staging_db"
db_username = "cafm_staging_user"

# ==============================================================================
# REDIS CONFIGURATION
# ==============================================================================
redis_node_type = "cache.t3.small"

# ==============================================================================
# MONITORING
# ==============================================================================
alert_email = "staging-alerts@cafm.company.com"

# ==============================================================================
# SECURITY
# ==============================================================================
enable_vpc_flow_logs = true
enable_guardduty     = false
enable_security_hub  = false
enable_config       = false
enable_cloudtrail   = true

encryption_at_rest    = true
encryption_in_transit = true

# ==============================================================================
# BACKUP AND DR
# ==============================================================================
backup_retention_days = 14
multi_az_deployment   = true
cross_region_backup   = false

backup_schedule = "cron(0 3 * * ? *)" # Daily at 3 AM UTC

# ==============================================================================
# PERFORMANCE
# ==============================================================================
enable_performance_insights = true
enhanced_monitoring         = true
enable_container_insights   = true

# ==============================================================================
# COST OPTIMIZATION
# ==============================================================================
use_spot_instances        = true
enable_storage_lifecycle  = true
single_nat_gateway        = false

# ==============================================================================
# FEATURE FLAGS
# ==============================================================================
enable_nat_gateway = true
enable_irsa       = true

# ==============================================================================
# ADDITIONAL TAGS
# ==============================================================================
additional_tags = {
  Environment   = "staging"
  CriticalLevel = "medium"
  Backup        = "optional"
  Monitoring    = "standard"
  Compliance    = "testing"
}