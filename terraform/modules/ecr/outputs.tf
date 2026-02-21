output "repository_urls" {
  description = "Map of service name → ECR repository URL. Use in ECS task definitions and GitHub Actions deploy workflows."
  value       = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}

output "repository_arns" {
  description = "Map of service name → ECR repository ARN. Use in IAM policies granting ECS task execution roles pull access."
  value       = { for k, v in aws_ecr_repository.services : k => v.arn }
}
