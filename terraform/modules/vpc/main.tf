# =============================================================================
# VPC Module - Network Infrastructure
# =============================================================================
# Creates:
#   - VPC with DNS support and DNS hostnames enabled
#   - 3 public subnets (one per AZ) for the Application Load Balancer
#   - 3 private subnets (one per AZ) for ECS Fargate tasks
#   - Internet Gateway attached to the VPC for public subnet outbound traffic
#   - One Elastic IP and NAT Gateway in the first public subnet for private
#     subnet outbound traffic (cost-optimised single NAT; for full HA use one
#     NAT Gateway per AZ)
#   - Public route table: 0.0.0.0/0 → Internet Gateway
#   - Private route table: 0.0.0.0/0 → NAT Gateway
# =============================================================================

locals {
  name_prefix = "${var.project_name}-${var.environment}"

  # Carve /24 subnets from the VPC CIDR using cidrsubnet():
  #   Public  subnets: .1.0/24, .2.0/24, .3.0/24   (for the ALB layer)
  #   Private subnets: .10.0/24, .11.0/24, .12.0/24 (for ECS task layer)
  public_subnet_cidrs  = [for i in range(3) : cidrsubnet(var.vpc_cidr, 8, i + 1)]
  private_subnet_cidrs = [for i in range(3) : cidrsubnet(var.vpc_cidr, 8, i + 10)]
}

# ---- VPC ----
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true   # Required for ECS container name resolution
  enable_dns_hostnames = true   # Required for ECR VPC endpoints (if added later)

  tags = { Name = "${local.name_prefix}-vpc" }
}

# ---- Internet Gateway ----
# Provides bidirectional internet connectivity for resources in public subnets.
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "${local.name_prefix}-igw" }
}

# ---- Public Subnets ----
# One per AZ; the ALB is spread across all three for high availability.
# map_public_ip_on_launch = true so the ALB can receive internet traffic.
resource "aws_subnet" "public" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = local.public_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  map_public_ip_on_launch = true

  tags = {
    Name = "${local.name_prefix}-public-${count.index + 1}"
    Tier = "public"
  }
}

# ---- Private Subnets ----
# One per AZ; ECS Fargate tasks run here and are unreachable from the internet.
# Outbound traffic is routed through the NAT Gateway.
resource "aws_subnet" "private" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = local.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name_prefix}-private-${count.index + 1}"
    Tier = "private"
  }
}

# ---- Elastic IP for NAT Gateway ----
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = { Name = "${local.name_prefix}-nat-eip" }

  # EIP must be created after the IGW is attached to the VPC
  depends_on = [aws_internet_gateway.main]
}

# ---- NAT Gateway ----
# Placed in the first public subnet. ECS tasks in private subnets use this
# gateway to pull images from ECR, reach MongoDB Atlas, publish CloudWatch
# metrics, and call the OSP mock API.
#
# Cost note: A single NAT Gateway is used for simplicity. For production
# high-availability, create one NAT Gateway per AZ and add separate private
# route tables for each AZ.
resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = { Name = "${local.name_prefix}-nat" }

  depends_on = [aws_internet_gateway.main]
}

# ---- Public Route Table ----
# All internet-bound traffic from public subnets exits through the IGW.
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "${local.name_prefix}-public-rt" }
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# ---- Private Route Table ----
# All internet-bound traffic from private subnets exits through the NAT Gateway.
# This allows ECS tasks to initiate outbound connections without being reachable
# from the internet.
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = { Name = "${local.name_prefix}-private-rt" }
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
