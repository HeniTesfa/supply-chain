# =============================================================================
# Root Module Outputs
# =============================================================================
# Key values emitted after a successful apply. These are used to configure
# GitHub Actions secrets and ECS task definitions.
# =============================================================================

# ---- VPC ----

output "vpc_id" {
  description = "ID of the VPC hosting the supply chain services."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "IDs of the three public subnets (one per AZ). Used for the Application Load Balancer."
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the three private subnets (one per AZ). Used for ECS Fargate task networking."
  value       = module.vpc.private_subnet_ids
}

output "nat_gateway_ip" {
  description = "Public Elastic IP of the NAT Gateway. Add to allowlists for external services that restrict inbound IPs."
  value       = module.vpc.nat_gateway_ip
}

# ---- Security Groups ----

output "alb_security_group_id" {
  description = "ID of the Application Load Balancer security group (accepts HTTP/HTTPS from internet)."
  value       = module.security_groups.alb_sg_id
}

output "ecs_tasks_security_group_id" {
  description = "ID of the ECS tasks security group. Attach this to every ECS service in the cluster."
  value       = module.security_groups.ecs_tasks_sg_id
}

output "mongodb_security_group_id" {
  description = "ID of the MongoDB security group (port 27017, ECS tasks only)."
  value       = module.security_groups.mongodb_sg_id
}

output "kafka_security_group_id" {
  description = "ID of the Kafka/Zookeeper security group (ports 9092/2181, ECS tasks only)."
  value       = module.security_groups.kafka_sg_id
}

# ---- ECR ----

output "ecr_repository_urls" {
  description = <<-EOT
    Map of service name → ECR repository URL for all 10 supply chain services.
    Use these URLs in ECS task definitions and the GitHub Actions deploy workflows.
    Example: { "consumer-service" = "123456789012.dkr.ecr.us-east-1.amazonaws.com/supply-chain-consumer-service" }
  EOT
  value = module.ecr.repository_urls
}

output "ecr_repository_arns" {
  description = "Map of service name → ECR repository ARN. Use in IAM policies granting ECS task roles pull access."
  value       = module.ecr.repository_arns
}
