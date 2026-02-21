# =============================================================================
# Root Module Variables
# =============================================================================
# All configurable values for the supply chain infrastructure.
# Override defaults by creating a terraform.tfvars file or by passing
# -var flags to terraform plan/apply.
# =============================================================================

variable "aws_region" {
  description = "AWS region in which all resources will be created (e.g. us-east-1)."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Short project identifier used as a prefix for all resource names and tags."
  type        = string
  default     = "supply-chain"
}

variable "environment" {
  description = "Deployment environment. Must be 'staging' or 'production'."
  type        = string

  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment must be either 'staging' or 'production'."
  }
}

variable "vpc_cidr" {
  description = "IPv4 CIDR block for the VPC. Must be a /16 to allow room for 6 subnets (/24 each)."
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = <<-EOT
    List of Availability Zone names to deploy subnets into.
    Must contain exactly 3 AZs that exist in var.aws_region.
    Example: ["us-east-1a", "us-east-1b", "us-east-1c"]
  EOT
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]

  validation {
    condition     = length(var.availability_zones) == 3
    error_message = "Exactly 3 availability zones must be specified."
  }
}

variable "ecr_image_retention_count" {
  description = "Maximum number of tagged images to retain per ECR repository. Older tagged images are expired by the lifecycle policy."
  type        = number
  default     = 10

  validation {
    condition     = var.ecr_image_retention_count >= 1
    error_message = "ecr_image_retention_count must be at least 1."
  }
}
