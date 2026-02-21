variable "project_name" {
  description = "Short project identifier used as a prefix for resource names."
  type        = string
}

variable "environment" {
  description = "Deployment environment (staging or production)."
  type        = string
}

variable "vpc_cidr" {
  description = "IPv4 CIDR block for the VPC (e.g. 10.0.0.0/16)."
  type        = string
}

variable "availability_zones" {
  description = "Ordered list of 3 AZ names to spread subnets across (e.g. [\"us-east-1a\",\"us-east-1b\",\"us-east-1c\"])."
  type        = list(string)
}
