variable "project_name" {
  description = "Short project identifier used as a prefix for security group names."
  type        = string
}

variable "environment" {
  description = "Deployment environment (staging or production)."
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC in which to create the security groups."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block of the VPC, used to allow intra-VPC service-to-service traffic."
  type        = string
}
