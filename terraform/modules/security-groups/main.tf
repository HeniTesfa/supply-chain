# =============================================================================
# Security Groups Module - Network Access Control
# =============================================================================
# Creates one security group per logical tier of the supply chain system:
#
#   alb_sg       – Application Load Balancer: accepts HTTP (80) and HTTPS (443)
#                  from the internet. All other traffic denied by default.
#
#   ecs_tasks_sg – All 9 microservices: accepts traffic from the ALB and intra-
#                  VPC service-to-service calls (e.g. loader → item-service).
#                  Full outbound allowed so tasks can reach MongoDB, Kafka, ECR,
#                  CloudWatch, and the OSP external API.
#
#   mongodb_sg   – MongoDB / DocumentDB: accepts port 27017 from ECS tasks only.
#                  No direct internet access to the data tier.
#
#   kafka_sg     – Kafka brokers + Zookeeper: accepts port 9092 (Kafka client)
#                  and 2181 (Zookeeper) from ECS tasks only. A separate
#                  aws_security_group_rule resource handles the self-referencing
#                  inter-broker replication port (9093) to avoid a circular
#                  dependency at creation time.
# =============================================================================

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# ---- Application Load Balancer Security Group ----
resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb-sg"
  description = "ALB: inbound HTTP/HTTPS from internet, outbound to ECS tasks."
  vpc_id      = var.vpc_id

  # HTTP — ALB listener rule can redirect to HTTPS
  ingress {
    description = "HTTP from internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS — TLS terminated at the ALB
  ingress {
    description = "HTTPS from internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Unrestricted outbound so the ALB can forward requests to any ECS task port
  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-alb-sg" }
}

# ---- ECS Tasks Security Group ----
# Shared by all 9 microservices (BFF :8080, consumer :8081, loader :8082,
# item :8083, trade-item :8084, supplier-supply :8085, shipment :8086,
# producer :8087, osp-mock-api :9000).
resource "aws_security_group" "ecs_tasks" {
  name        = "${local.name_prefix}-ecs-tasks-sg"
  description = "ECS tasks: inbound from ALB and intra-VPC; outbound unrestricted."
  vpc_id      = var.vpc_id

  # Traffic forwarded by the ALB to backend ECS services
  ingress {
    description     = "From ALB to ECS service ports"
    from_port       = 8080
    to_port         = 9000
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Service-to-service calls within the VPC
  # (e.g. bff-service → producer-service, loader-service → item-service)
  ingress {
    description = "Intra-VPC service-to-service traffic"
    from_port   = 8080
    to_port     = 9000
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # Unrestricted outbound — tasks must reach MongoDB, Kafka, ECR, CloudWatch,
  # the OSP API, Datadog, and Dynatrace endpoints
  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-ecs-tasks-sg" }
}

# ---- MongoDB Security Group ----
# Accepts MongoDB wire protocol connections only from ECS task containers.
resource "aws_security_group" "mongodb" {
  name        = "${local.name_prefix}-mongodb-sg"
  description = "MongoDB: inbound port 27017 from ECS tasks only."
  vpc_id      = var.vpc_id

  ingress {
    description     = "MongoDB from ECS tasks"
    from_port       = 27017
    to_port         = 27017
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-mongodb-sg" }
}

# ---- Kafka / Zookeeper Security Group ----
resource "aws_security_group" "kafka" {
  name        = "${local.name_prefix}-kafka-sg"
  description = "Kafka/Zookeeper: ports 9092 and 2181 from ECS tasks; 9093 inter-broker (self-referencing rule added separately)."
  vpc_id      = var.vpc_id

  # Kafka client port — consumer-service and producer-service connect here
  ingress {
    description     = "Kafka broker from ECS tasks"
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  # Zookeeper client port — Kafka brokers use this for cluster coordination
  ingress {
    description     = "Zookeeper from ECS tasks"
    from_port       = 2181
    to_port         = 2181
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-kafka-sg" }
}

# ---- Kafka Inter-Broker Replication Rule ----
# This rule references the kafka security group as both source and destination.
# It must be a separate resource (not inline ingress block) because Terraform
# cannot resolve the self-reference during the initial create of the SG resource.
resource "aws_security_group_rule" "kafka_interbroker" {
  type                     = "ingress"
  description              = "Kafka inter-broker replication (port 9093)"
  from_port                = 9093
  to_port                  = 9093
  protocol                 = "tcp"
  security_group_id        = aws_security_group.kafka.id
  source_security_group_id = aws_security_group.kafka.id
}
