# Development Environment Configuration
# This file contains dev-specific variable values

# ==============================================================================
# GENERAL CONFIGURATION
# ==============================================================================
environment = "dev"
aws_region  = "us-west-2"

# ==============================================================================
# NETWORKING
# ==============================================================================
vpc_cidr = "10.2.0.0/16"

allowed_origins = [
  "http://localhost:3000",
  "http://localhost:8080",
  "https://dev.cafm.company.com",
  "https://dev-app.cafm.company.com"
]

# ==============================================================================
# EKS CLUSTER CONFIGURATION
# ==============================================================================
kubernetes_version = "1.29"

eks_node_instance_types = ["t3.small", "t3.medium"]
eks_node_min_size       = 1
eks_node_max_size       = 4
eks_node_desired_size   = 2

# ==============================================================================
# DATABASE CONFIGURATION
# ==============================================================================
rds_instance_class      = "db.t3.micro"
rds_allocated_storage   = 20
rds_max_allocated_storage = 50

db_name     = "cafm_dev_db"
db_username = "cafm_dev_user"

# ==============================================================================
# REDIS CONFIGURATION
# ==============================================================================
redis_node_type = "cache.t3.micro"

# ==============================================================================
# MONITORING
# ==============================================================================
alert_email = "dev-alerts@cafm.company.com"

# ==============================================================================
# SECURITY
# ==============================================================================
enable_vpc_flow_logs = false
enable_guardduty     = false
enable_security_hub  = false
enable_config       = false
enable_cloudtrail   = false

encryption_at_rest    = false
encryption_in_transit = false

# ==============================================================================
# BACKUP AND DR
# ==============================================================================
backup_retention_days = 3
multi_az_deployment   = false
cross_region_backup   = false

backup_schedule = "cron(0 4 * * ? *)" # Daily at 4 AM UTC

# ==============================================================================
# PERFORMANCE
# ==============================================================================
enable_performance_insights = false
enhanced_monitoring         = false
enable_container_insights   = false

# ==============================================================================
# COST OPTIMIZATION
# ==============================================================================
use_spot_instances        = true
enable_storage_lifecycle  = false
single_nat_gateway        = true

# ==============================================================================
# FEATURE FLAGS
# ==============================================================================
enable_nat_gateway = true
enable_irsa       = true

# ==============================================================================
# ADDITIONAL TAGS
# ==============================================================================
additional_tags = {
  Environment   = "development"
  CriticalLevel = "low"
  Backup        = "not-required"
  Monitoring    = "basic"
  Compliance    = "none"
  AutoShutdown  = "enabled"
}