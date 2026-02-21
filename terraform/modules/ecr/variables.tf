variable "project_name" {
  description = "Short project identifier prepended to every ECR repository name (e.g. supply-chain)."
  type        = string
}

variable "environment" {
  description = "Deployment environment (staging or production). Added as a tag to all repositories."
  type        = string
}

variable "image_retention_count" {
  description = "Maximum number of tagged images to retain per repository. Images beyond this limit are expired by the lifecycle policy."
  type        = number
  default     = 10
}
