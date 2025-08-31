# CAFM Backend Infrastructure
# This file defines the main infrastructure components for the CAFM backend application

terraform {
  required_version = ">= 1.6"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.20"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.10"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }
  
  backend "s3" {
    # Backend configuration will be provided during init
    # terraform init -backend-config="bucket=cafm-terraform-state-${environment}" \
    #                -backend-config="key=cafm-backend/terraform.tfstate" \
    #                -backend-config="region=us-west-2"
  }
}

# Configure the AWS Provider
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "CAFM Backend"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Owner       = "CAFM Team"
      CostCenter  = "Engineering"
    }
  }
}

# Data sources for existing infrastructure
data "aws_caller_identity" "current" {}
data "aws_availability_zones" "available" {
  state = "available"
}

# Local values for resource naming and configuration
locals {
  name_prefix = "cafm-${var.environment}"
  common_tags = {
    Project     = "CAFM Backend"
    Environment = var.environment
    Application = "cafm-backend"
  }
  
  # Calculate subnet CIDRs
  vpc_cidr = var.vpc_cidr
  azs      = slice(data.aws_availability_zones.available.names, 0, 3)
  
  # Database configuration
  db_subnet_group_name = "${local.name_prefix}-db-subnet-group"
  
  # EKS configuration
  cluster_name = "${local.name_prefix}-cluster"
}

# ==============================================================================
# NETWORKING
# ==============================================================================
module "vpc" {
  source = "./modules/vpc"
  
  name_prefix = local.name_prefix
  vpc_cidr    = local.vpc_cidr
  azs         = local.azs
  
  # Public subnets for load balancers and NAT gateways
  public_subnet_cidrs = [
    cidrsubnet(local.vpc_cidr, 8, 1),  # 10.0.1.0/24
    cidrsubnet(local.vpc_cidr, 8, 2),  # 10.0.2.0/24
    cidrsubnet(local.vpc_cidr, 8, 3),  # 10.0.3.0/24
  ]
  
  # Private subnets for EKS worker nodes
  private_subnet_cidrs = [
    cidrsubnet(local.vpc_cidr, 8, 10), # 10.0.10.0/24
    cidrsubnet(local.vpc_cidr, 8, 11), # 10.0.11.0/24
    cidrsubnet(local.vpc_cidr, 8, 12), # 10.0.12.0/24
  ]
  
  # Database subnets
  database_subnet_cidrs = [
    cidrsubnet(local.vpc_cidr, 8, 20), # 10.0.20.0/24
    cidrsubnet(local.vpc_cidr, 8, 21), # 10.0.21.0/24
    cidrsubnet(local.vpc_cidr, 8, 22), # 10.0.22.0/24
  ]
  
  enable_nat_gateway = true
  single_nat_gateway = var.environment != "production"
  enable_vpn_gateway = false
  
  tags = local.common_tags
}

# ==============================================================================
# EKS CLUSTER
# ==============================================================================
module "eks" {
  source = "./modules/eks"
  
  cluster_name = local.cluster_name
  vpc_id       = module.vpc.vpc_id
  subnet_ids   = module.vpc.private_subnet_ids
  
  # Cluster configuration
  cluster_version = var.kubernetes_version
  
  # Node group configuration
  node_groups = {
    main = {
      instance_types = var.eks_node_instance_types
      capacity_type  = "ON_DEMAND"
      
      min_size     = var.eks_node_min_size
      max_size     = var.eks_node_max_size
      desired_size = var.eks_node_desired_size
      
      k8s_labels = {
        Environment = var.environment
        NodeGroup   = "main"
      }
    }
    
    # Spot instances for non-critical workloads in non-production
    spot = var.environment != "production" ? {
      instance_types = ["t3.medium", "t3.large"]
      capacity_type  = "SPOT"
      
      min_size     = 0
      max_size     = 5
      desired_size = 1
      
      k8s_labels = {
        Environment = var.environment
        NodeGroup   = "spot"
      }
      
      taints = [
        {
          key    = "spot"
          value  = "true"
          effect = "NO_SCHEDULE"
        }
      ]
    } : null
  }
  
  tags = local.common_tags
}

# ==============================================================================
# RDS DATABASE
# ==============================================================================
module "rds" {
  source = "./modules/rds"
  
  identifier = "${local.name_prefix}-postgres"
  
  # Database configuration
  engine         = "postgres"
  engine_version = "15.6"
  instance_class = var.rds_instance_class
  
  allocated_storage     = var.rds_allocated_storage
  max_allocated_storage = var.rds_max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true
  
  # Database credentials
  db_name  = var.db_name
  username = var.db_username
  
  # Network configuration
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.database_subnet_ids
  
  # Security
  allowed_cidr_blocks = [module.vpc.vpc_cidr_block]
  
  # Backup configuration
  backup_retention_period = var.environment == "production" ? 30 : 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  # Performance Insights
  performance_insights_enabled = var.environment == "production"
  monitoring_interval         = var.environment == "production" ? 60 : 0
  
  # Multi-AZ for production
  multi_az = var.environment == "production"
  
  tags = local.common_tags
}

# ==============================================================================
# ELASTICACHE REDIS
# ==============================================================================
module "redis" {
  source = "./modules/redis"
  
  cluster_id = "${local.name_prefix}-redis"
  
  # Redis configuration
  node_type           = var.redis_node_type
  num_cache_nodes     = var.environment == "production" ? 3 : 1
  port                = 6379
  parameter_group     = "default.redis7"
  engine_version      = "7.0"
  
  # Network configuration
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnet_ids
  
  # Security
  allowed_cidr_blocks = [module.vpc.vpc_cidr_block]
  
  # Backup
  snapshot_retention_limit = var.environment == "production" ? 7 : 1
  snapshot_window         = "03:00-05:00"
  
  # Encryption
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  tags = local.common_tags
}

# ==============================================================================
# S3 STORAGE
# ==============================================================================
module "s3" {
  source = "./modules/s3"
  
  bucket_name = "${local.name_prefix}-storage"
  
  # Versioning
  versioning_enabled = var.environment == "production"
  
  # Lifecycle configuration
  lifecycle_rules = [
    {
      id     = "transition_to_ia"
      status = "Enabled"
      
      transition = [
        {
          days          = 30
          storage_class = "STANDARD_IA"
        },
        {
          days          = 90
          storage_class = "GLACIER"
        }
      ]
      
      expiration = var.environment == "production" ? null : {
        days = 90
      }
    }
  ]
  
  # CORS configuration for file uploads
  cors_rules = [
    {
      allowed_headers = ["*"]
      allowed_methods = ["PUT", "POST", "GET", "DELETE"]
      allowed_origins = var.allowed_origins
      max_age_seconds = 3000
    }
  ]
  
  tags = local.common_tags
}

# ==============================================================================
# SECRETS MANAGER
# ==============================================================================
resource "aws_secretsmanager_secret" "database" {
  name                    = "${local.name_prefix}/database"
  description            = "Database credentials for CAFM Backend"
  recovery_window_in_days = var.environment == "production" ? 30 : 0
  
  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "database" {
  secret_id = aws_secretsmanager_secret.database.id
  secret_string = jsonencode({
    username = module.rds.db_username
    password = module.rds.db_password
    host     = module.rds.db_endpoint
    port     = module.rds.db_port
    dbname   = module.rds.db_name
  })
}

resource "aws_secretsmanager_secret" "redis" {
  name                    = "${local.name_prefix}/redis"
  description            = "Redis credentials for CAFM Backend"
  recovery_window_in_days = var.environment == "production" ? 30 : 0
  
  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "redis" {
  secret_id = aws_secretsmanager_secret.redis.id
  secret_string = jsonencode({
    host = module.redis.cache_endpoint
    port = module.redis.cache_port
    auth_token = module.redis.auth_token
  })
}

# Generate JWT secret
resource "random_password" "jwt_secret" {
  length  = 64
  special = true
}

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${local.name_prefix}/jwt"
  description            = "JWT secret for CAFM Backend"
  recovery_window_in_days = var.environment == "production" ? 30 : 0
  
  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id = aws_secretsmanager_secret.jwt.id
  secret_string = jsonencode({
    secret = random_password.jwt_secret.result
  })
}

# ==============================================================================
# IAM ROLES FOR EKS WORKLOADS
# ==============================================================================
module "iam" {
  source = "./modules/iam"
  
  cluster_name = local.cluster_name
  environment  = var.environment
  
  # OIDC provider for IRSA
  oidc_provider_arn = module.eks.cluster_oidc_provider_arn
  
  tags = local.common_tags
}

# ==============================================================================
# MONITORING AND OBSERVABILITY
# ==============================================================================
module "monitoring" {
  source = "./modules/monitoring"
  
  name_prefix = local.name_prefix
  
  # CloudWatch configuration
  log_retention_days = var.environment == "production" ? 90 : 30
  
  # SNS topic for alerts
  alert_email = var.alert_email
  
  tags = local.common_tags
}

# ==============================================================================
# BACKUP CONFIGURATION
# ==============================================================================
module "backup" {
  source = "./modules/backup"
  count  = var.environment == "production" ? 1 : 0
  
  name_prefix = local.name_prefix
  
  # Backup targets
  rds_arn = module.rds.db_arn
  s3_arn  = module.s3.bucket_arn
  
  # Backup schedule
  backup_schedule = "cron(0 2 * * ? *)" # Daily at 2 AM
  
  # Retention
  delete_after_days = 30
  
  tags = local.common_tags
}