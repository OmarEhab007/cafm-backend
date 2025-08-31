# Production Environment Configuration
# This file contains production-specific variable values

# ==============================================================================
# GENERAL CONFIGURATION
# ==============================================================================
environment = "production"
aws_region  = "us-west-2"

# ==============================================================================
# NETWORKING
# ==============================================================================
vpc_cidr = "10.0.0.0/16"

allowed_origins = [
  "https://cafm.company.com",
  "https://app.cafm.company.com",
  "https://admin.cafm.company.com"
]

# ==============================================================================
# EKS CLUSTER CONFIGURATION
# ==============================================================================
kubernetes_version = "1.29"

eks_node_instance_types = ["m5.large", "m5.xlarge"]
eks_node_min_size       = 3
eks_node_max_size       = 20
eks_node_desired_size   = 5

# ==============================================================================
# DATABASE CONFIGURATION
# ==============================================================================
rds_instance_class      = "db.r5.large"
rds_allocated_storage   = 100
rds_max_allocated_storage = 1000

db_name     = "cafm_prod_db"
db_username = "cafm_prod_user"

# ==============================================================================
# REDIS CONFIGURATION
# ==============================================================================
redis_node_type = "cache.r5.large"

# ==============================================================================
# MONITORING
# ==============================================================================
alert_email = "alerts@cafm.company.com"

# ==============================================================================
# SECURITY
# ==============================================================================
enable_vpc_flow_logs = true
enable_guardduty     = true
enable_security_hub  = true
enable_config       = true
enable_cloudtrail   = true

encryption_at_rest    = true
encryption_in_transit = true

# ==============================================================================
# BACKUP AND DR
# ==============================================================================
backup_retention_days = 30
multi_az_deployment   = true
cross_region_backup   = true

backup_schedule = "cron(0 2 * * ? *)" # Daily at 2 AM UTC

# ==============================================================================
# PERFORMANCE
# ==============================================================================
enable_performance_insights = true
enhanced_monitoring         = true
enable_container_insights   = true

# ==============================================================================
# COST OPTIMIZATION
# ==============================================================================
use_spot_instances        = false
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
  Environment   = "production"
  CriticalLevel = "high"
  Backup        = "required"
  Monitoring    = "enhanced"
  Compliance    = "required"
}