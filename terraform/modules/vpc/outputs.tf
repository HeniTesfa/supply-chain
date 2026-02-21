output "vpc_id" {
  description = "ID of the created VPC."
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of the three public subnets (one per AZ). Pass to the ALB resource."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the three private subnets (one per AZ). Pass to ECS service network configuration."
  value       = aws_subnet.private[*].id
}

output "nat_gateway_ip" {
  description = "Public Elastic IP address of the NAT Gateway. Add to allowlists of any external services that restrict incoming IPs (e.g. MongoDB Atlas, Dynatrace, Datadog)."
  value       = aws_eip.nat.public_ip
}
