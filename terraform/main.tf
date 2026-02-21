# =============================================================================
# Root Module - Supply Chain AWS Infrastructure
# =============================================================================
# Orchestrates the three child modules that together form the infrastructure
# required to run the supply chain microservices on AWS ECS Fargate:
#
#   vpc             – VPC, public/private subnets, IGW, NAT GW, route tables
#   security-groups – ALB, ECS tasks, MongoDB, and Kafka security groups
#   ecr             – One ECR repository per service with lifecycle policies
#
# Dependency graph:
#   vpc → security-groups (needs vpc_id and vpc_cidr)
#   ecr  (independent of vpc and security-groups)
# =============================================================================

# ---- VPC ----
# Creates the network foundation: VPC, 3 public subnets (ALB), 3 private
# subnets (ECS tasks), Internet Gateway, NAT Gateway, and route tables.
module "vpc" {
  source = "./modules/vpc"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
}

# ---- Security Groups ----
# Creates one security group per logical tier (ALB, ECS tasks, MongoDB, Kafka).
# Depends on the VPC module for the vpc_id and vpc_cidr values.
module "security_groups" {
  source = "./modules/security-groups"

  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id
  vpc_cidr     = var.vpc_cidr
}

# ---- ECR Repositories ----
# Creates one ECR repository for each of the 10 supply chain services.
# Independent of vpc and security_groups — image push/pull happens over
# the internet (or via VPC endpoints, added later if needed).
module "ecr" {
  source = "./modules/ecr"

  project_name          = var.project_name
  environment           = var.environment
  image_retention_count = var.ecr_image_retention_count
}
