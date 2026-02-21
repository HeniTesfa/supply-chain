# =============================================================================
# Terraform Version Constraints and Provider Configuration
# =============================================================================
# Declares the minimum Terraform CLI version and the required AWS provider.
# The S3 backend block is intentionally empty so the same code works across
# environments — supply backend values via:
#
#   terraform init \
#     -backend-config="bucket=<tfstate-bucket>" \
#     -backend-config="key=supply-chain/<env>/terraform.tfstate" \
#     -backend-config="region=<aws-region>" \
#     -backend-config="dynamodb_table=<tfstate-lock-table>"
# =============================================================================

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state stored in S3 with DynamoDB locking.
  # Configure via -backend-config flags at init time (see header comment).
  backend "s3" {}
}

# ---- AWS Provider ----
# The region is driven by the aws_region variable so the same Terraform code
# can target us-east-1 (staging) or any other region (production).
# Default tags are injected into every resource created by this provider.
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Repository  = "supply-chain-complete"
    }
  }
}
