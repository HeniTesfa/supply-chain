# =============================================================================
# ECR Module - Container Image Repositories
# =============================================================================
# Creates one Amazon ECR repository for each of the 10 supply chain services.
# Repository names follow the pattern: <project_name>-<service_name>
# (e.g. supply-chain-consumer-service), matching the naming convention used
# in the GitHub Actions deploy workflows.
#
# Each repository is configured with:
#   - Image vulnerability scanning on every push
#   - Mutable image tags (allows overwriting :latest and commit-SHA tags)
#   - A lifecycle policy with two rules:
#       Rule 1: Expire untagged images after 1 day (cleans up dangling layers)
#       Rule 2: Keep only the N most recent tagged images (bounds storage cost)
# =============================================================================

locals {
  # All 10 services that have Dockerfiles and are deployed as container images.
  # Names match the docker-compose service names and GitHub Actions matrix values.
  services = toset([
    "consumer-service",
    "loader-service",
    "item-service",
    "trade-item-service",
    "supplier-supply-service",
    "shipment-service",
    "producer-service",
    "osp-mock-api",
    "producer-ui",
    "monitoring-ui",
  ])
}

# ---- ECR Repositories ----
resource "aws_ecr_repository" "services" {
  for_each = local.services

  # Naming convention: supply-chain-<service-name>
  # Matches image tags in deploy-staging.yml and deploy-production.yml:
  #   ${{ env.ECR_REGISTRY }}/supply-chain-${{ matrix.service }}:${{ env.IMAGE_TAG }}
  name = "${var.project_name}-${each.key}"

  # MUTABLE allows the GitHub Actions workflows to push :latest and :<commit-sha>
  # tags repeatedly without errors. Commit SHA tags are never re-used in practice.
  image_tag_mutability = "MUTABLE"

  # Scan every image on push. Results appear in the AWS ECR console under
  # Images > Scan results and can be queried via the AWS CLI.
  image_scanning_configuration {
    scan_on_push = true
  }

  # Encryption uses the default AWS-managed key. Switch to aws:kms with a
  # customer-managed key for environments that require it.
  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Service = each.key
  }
}

# ---- ECR Lifecycle Policies ----
# Applied to every repository to prevent unbounded image accumulation.
resource "aws_ecr_lifecycle_policy" "services" {
  for_each = aws_ecr_repository.services

  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        # Rule 1 (highest priority): immediately expire images that lost their tag.
        # This happens when a new image is pushed with the :latest tag, making the
        # previous :latest image untagged. Without this rule those layers accumulate.
        rulePriority = 1
        description  = "Expire untagged (dangling) images after 1 day"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 1
        }
        action = { type = "expire" }
      },
      {
        # Rule 2: keep only the N most recent tagged images across all prefixes.
        # Commit-SHA-tagged images grow with every deployment; this caps the total.
        rulePriority = 2
        description  = "Keep the ${var.image_retention_count} most recent tagged images"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["latest", "sha", "v"]
          countType     = "imageCountMoreThan"
          countNumber   = var.image_retention_count
        }
        action = { type = "expire" }
      }
    ]
  })
}
