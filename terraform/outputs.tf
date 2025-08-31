# Outputs for CAFM Backend Infrastructure
# This file defines the outputs that will be available after terraform apply

# ==============================================================================
# VPC OUTPUTS
# ==============================================================================
output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.vpc.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = module.vpc.private_subnet_ids
}

output "database_subnet_ids" {
  description = "IDs of the database subnets"
  value       = module.vpc.database_subnet_ids
}

# ==============================================================================
# EKS OUTPUTS
# ==============================================================================
output "cluster_name" {
  description = "Name of the EKS cluster"
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "Endpoint for EKS control plane"
  value       = module.eks.cluster_endpoint
}

output "cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = module.eks.cluster_security_group_id
}

output "cluster_iam_role_name" {
  description = "IAM role name associated with EKS cluster"
  value       = module.eks.cluster_iam_role_name
}

output "cluster_iam_role_arn" {
  description = "IAM role ARN associated with EKS cluster"
  value       = module.eks.cluster_iam_role_arn
}

output "cluster_certificate_authority_data" {
  description = "Base64 encoded certificate data required to communicate with the cluster"
  value       = module.eks.cluster_certificate_authority_data
  sensitive   = true
}

output "cluster_oidc_provider_arn" {
  description = "The ARN of the OIDC Provider if IRSA is enabled"
  value       = module.eks.cluster_oidc_provider_arn
}

output "node_groups" {
  description = "EKS node groups"
  value       = module.eks.node_groups
  sensitive   = true
}

# ==============================================================================
# DATABASE OUTPUTS
# ==============================================================================
output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = module.rds.db_endpoint
  sensitive   = true
}

output "rds_port" {
  description = "RDS instance port"
  value       = module.rds.db_port
}

output "rds_database_name" {
  description = "RDS database name"
  value       = module.rds.db_name
}

output "rds_username" {
  description = "RDS database username"
  value       = module.rds.db_username
  sensitive   = true
}

output "rds_password" {
  description = "RDS database password"
  value       = module.rds.db_password
  sensitive   = true
}

output "rds_arn" {
  description = "RDS database ARN"
  value       = module.rds.db_arn
}

# ==============================================================================
# REDIS OUTPUTS
# ==============================================================================
output "redis_endpoint" {
  description = "Redis cache endpoint"
  value       = module.redis.cache_endpoint
  sensitive   = true
}

output "redis_port" {
  description = "Redis cache port"
  value       = module.redis.cache_port
}

output "redis_auth_token" {
  description = "Redis auth token"
  value       = module.redis.auth_token
  sensitive   = true
}

# ==============================================================================
# S3 OUTPUTS
# ==============================================================================
output "s3_bucket_name" {
  description = "Name of the S3 bucket"
  value       = module.s3.bucket_name
}

output "s3_bucket_arn" {
  description = "ARN of the S3 bucket"
  value       = module.s3.bucket_arn
}

output "s3_bucket_regional_domain_name" {
  description = "Regional domain name of the S3 bucket"
  value       = module.s3.bucket_regional_domain_name
}

# ==============================================================================
# SECRETS MANAGER OUTPUTS
# ==============================================================================
output "database_secret_arn" {
  description = "ARN of the database secret in Secrets Manager"
  value       = aws_secretsmanager_secret.database.arn
}

output "redis_secret_arn" {
  description = "ARN of the Redis secret in Secrets Manager"
  value       = aws_secretsmanager_secret.redis.arn
}

output "jwt_secret_arn" {
  description = "ARN of the JWT secret in Secrets Manager"
  value       = aws_secretsmanager_secret.jwt.arn
}

# ==============================================================================
# IAM OUTPUTS
# ==============================================================================
output "pod_execution_role_arn" {
  description = "ARN of the IAM role for pod execution"
  value       = module.iam.pod_execution_role_arn
}

output "external_secrets_role_arn" {
  description = "ARN of the IAM role for External Secrets Operator"
  value       = module.iam.external_secrets_role_arn
}

output "load_balancer_controller_role_arn" {
  description = "ARN of the IAM role for AWS Load Balancer Controller"
  value       = module.iam.load_balancer_controller_role_arn
}

# ==============================================================================
# MONITORING OUTPUTS
# ==============================================================================
output "cloudwatch_log_group_name" {
  description = "Name of the CloudWatch log group"
  value       = module.monitoring.log_group_name
}

output "sns_topic_arn" {
  description = "ARN of the SNS topic for alerts"
  value       = module.monitoring.sns_topic_arn
}

# ==============================================================================
# KUBECTL CONFIGURATION
# ==============================================================================
output "kubectl_config" {
  description = "kubectl config command to configure access to the EKS cluster"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

# ==============================================================================
# APPLICATION CONFIGURATION
# ==============================================================================
output "application_config" {
  description = "Configuration values for the application deployment"
  value = {
    environment = var.environment
    region      = var.aws_region
    
    database = {
      host     = module.rds.db_endpoint
      port     = module.rds.db_port
      name     = module.rds.db_name
      username = module.rds.db_username
    }
    
    redis = {
      host = module.redis.cache_endpoint
      port = module.redis.cache_port
    }
    
    storage = {
      bucket_name = module.s3.bucket_name
      region      = var.aws_region
    }
    
    secrets = {
      database_secret_arn = aws_secretsmanager_secret.database.arn
      redis_secret_arn    = aws_secretsmanager_secret.redis.arn
      jwt_secret_arn      = aws_secretsmanager_secret.jwt.arn
    }
    
    cluster = {
      name     = module.eks.cluster_name
      endpoint = module.eks.cluster_endpoint
    }
  }
  sensitive = true
}

# ==============================================================================
# COST ESTIMATION
# ==============================================================================
output "estimated_monthly_costs" {
  description = "Estimated monthly costs for the infrastructure"
  value = {
    notes = [
      "Cost estimates are approximate and based on us-west-2 pricing",
      "Actual costs may vary based on usage patterns",
      "Consider using AWS Cost Explorer for detailed cost analysis"
    ]
    
    eks_cluster         = "$72.00"  # $0.10 per hour
    eks_nodes          = local.env_costs[var.environment].eks_nodes
    rds                = local.env_costs[var.environment].rds
    redis              = local.env_costs[var.environment].redis
    s3                 = "$5.00 - $50.00 (usage-based)"
    nat_gateway        = var.single_nat_gateway ? "$45.00" : "$135.00"
    load_balancer      = "$16.20"  # Classic Load Balancer
    cloudwatch_logs    = "$0.50 - $10.00 (usage-based)"
    secrets_manager    = "$1.20"   # 3 secrets * $0.40
    
    total_estimated = local.env_costs[var.environment].total
  }
}

# Local values for cost estimation
locals {
  env_costs = {
    dev = {
      eks_nodes = "$30.00 - $90.00 (1-3 t3.medium)"
      rds       = "$15.00 (db.t3.micro)"
      redis     = "$15.00 (cache.t3.micro)"
      total     = "$200 - $300 per month"
    }
    
    staging = {
      eks_nodes = "$60.00 - $150.00 (2-5 t3.medium)"
      rds       = "$30.00 (db.t3.small)"
      redis     = "$30.00 (cache.t3.small)"
      total     = "$350 - $500 per month"
    }
    
    production = {
      eks_nodes = "$300.00 - $1200.00 (3-20 mixed instances)"
      rds       = "$200.00 (db.r5.large Multi-AZ)"
      redis     = "$200.00 (cache.r5.large with cluster)"
      total     = "$1000 - $2000 per month"
    }
  }
}