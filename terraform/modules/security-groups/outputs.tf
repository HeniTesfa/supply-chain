output "alb_sg_id" {
  description = "ID of the Application Load Balancer security group."
  value       = aws_security_group.alb.id
}

output "ecs_tasks_sg_id" {
  description = "ID of the ECS tasks security group. Attach to every ECS service in the cluster."
  value       = aws_security_group.ecs_tasks.id
}

output "mongodb_sg_id" {
  description = "ID of the MongoDB / DocumentDB security group."
  value       = aws_security_group.mongodb.id
}

output "kafka_sg_id" {
  description = "ID of the Kafka / Zookeeper security group."
  value       = aws_security_group.kafka.id
}
