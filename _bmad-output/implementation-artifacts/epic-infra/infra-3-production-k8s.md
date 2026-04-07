---
story_id: "infra-3"
epic: "infra"
story_key: "infra-3-production-k8s"
title: "Production Kubernetes Cluster và Deployment"
status: "ready-for-dev"
priority: "P4"
created_date: "2026-04-01"
input_artifacts: ["prd.md", "architecture.md"]
---

# Story: Production Kubernetes Cluster và Deployment

## User Story Statement

As a nhóm phát triển,
I want có production environment trên Kubernetes,
So that có thể học và thực hành DevOps skills với container orchestration chuyên nghiệp.

## Business Value

- Production-grade infrastructure
- Auto-scaling capabilities
- High availability
- Learning opportunity cho team
- Industry-relevant skills

## Technical Context

### Target: Learn DevOps với K8s
- Không phải cho production thực tế (overkill)
- Mục đích: học và practice DevOps skills

### Proposed Stack
- **Cluster**: DigitalOcean Kubernetes (DOK) - $12-24/mo
- **Ingress**: Nginx Ingress Controller
- **SSL**: Cert-manager + Let's Encrypt
- **Storage**: DigitalOcean Spaces (S3-compatible)
- **Database**: Neon production branch (vẫn cloud)

### Alternative Learning Options
- Minikube (local, free)
- kind (local, free)
- GCP E2 micro (1 node, free tier)

## Requirements

### Functional Requirements
1. **FR-Infra.3.1**: Setup Kubernetes cluster (DOK hoặc local)
2. **FR-Infra.3.2**: Configure kubectl và access cluster
3. **FR-Infra.3.3**: Create Kubernetes manifests cho API và Web
4. **FR-Infra.3.4**: Configure Nginx Ingress với SSL
5. **FR-Infra.3.5**: Setup auto-scaling (HPA)
6. **FR-Infra.3.6**: Configure secrets management (k8s secrets hoặc Vault)
7. **FR-Infra.3.7**: Setup logging và monitoring (basic)
8. **FR-Infra.3.8**: Document deployment process

### Non-Functional Requirements
- **NFR-Infra.3.1**: Cluster uptime >= 99.9%
- **NFR-Infra.3.2**: Zero-downtime deployments
- **NFR-Infra.3.3**: Resource limits configured

## Acceptance Criteria

### Given
K8s cluster created

### When
Deploy application

### Then
- [ ] API pods running và healthy
- [ ] Web pods running và healthy
- [ ] Ingress configured với SSL
- [ ] Services accessible via domain
- [ ] Auto-scaling works

### Given
Production running on K8s

### When
New version deployed

### Then
- [ ] Rolling update works
- [ ] No downtime
- [ ] Health checks pass

## Implementation Details

### K8s Manifest Structure

```
k8s/
├── base/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   └── configmap.yaml
├── overlays/
│   ├── staging/
│   └── production/
└── README.md
```

### Deployment Manifest

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: healthlens-api
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
  template:
    spec:
      containers:
      - name: api
        image: ghcr.io/ie303/healthlens-api:latest
        ports:
        - containerPort: 8080
        resources:
          limits:
            cpu: "1000m"
            memory: "1Gi"
```

### Ingress Configuration

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: healthlens-ingress
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - healthlens.app
    - api.healthlens.app
    secretName: healthlens-tls
  rules:
  - host: healthlens.app
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: healthlens-web
            port:
              number: 80
```

### Resource Planning

| Component | CPU | Memory | Replicas |
|-----------|-----|--------|----------|
| API | 500m | 512Mi | 2-5 (HPA) |
| Web | 250m | 256Mi | 2-5 (HPA) |
| Ingress | 100m | 128Mi | 1 |

## Learning Path (Optional)

```
Week 1: Local K8s (Minikube/kind)
  - Understand pods, services, deployments
  - Try kubectl commands

Week 2: Cloud K8s (DOK)
  - Create managed cluster
  - Deploy sample app

Week 3: Production Features
  - Ingress + SSL
  - Auto-scaling
  - Monitoring

Week 4: GitOps
  - ArgoCD integration
  - Git-based deployment
```

## Dependencies

- **Pre-requisite**: Docker knowledge (Infra.1)
- **Pre-requisite**: CI/CD pipeline (Infra.4)
- **Optional**: DOK account ($12-24/mo) hoặc local Minikube (free)

## Testing Checklist

- [ ] Cluster accessible
- [ ] Manifests valid
- [ ] Deployments successful
- [ ] Ingress works
- [ ] SSL configured
- [ ] Auto-scaling configured

## Notes

- Production với K8s có thể overkill cho startup
- Consider using managed services (Vercel/Railway) for actual production
- K8s là valuable skill cho career