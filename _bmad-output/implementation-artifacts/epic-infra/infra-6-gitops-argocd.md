---
story_id: "infra-6"
epic: "infra"
story_key: "infra-6-gitops-argocd"
title: "GitOps với ArgoCD"
status: "ready-for-dev"
priority: "P5"
created_date: "2026-04-01"
input_artifacts: ["architecture.md", "infra-3-production-k8s.md"]
---

# Story: GitOps với ArgoCD

## User Story Statement

As a nhóm DevOps,
I want có GitOps workflow với ArgoCD,
So that deployment được quản lý hoàn toàn qua Git, đảm bảo traceability và rollback dễ dàng.

## Business Value

- Declarative infrastructure
- Single source of truth cho deployments
- Easy rollback to previous versions
- Audit trail via Git history
- Self-healing infrastructure

## Technical Context

### Pre-requisites
- K8s cluster running (Infra.3)
- Git repository với k8s manifests

### ArgoCD Overview
- GitOps tool cho K8s
- watches Git repo và syncs to cluster
- Auto-reconcile when Git changes
- Visual dashboard

## Requirements

### Functional Requirements
1. **FR-Infra.6.1**: Install ArgoCD on K8s cluster
2. **FR-Infra.6.2**: Configure ArgoCD Application for each service
3. **FR-Infra.6.3**: Setup Git repository as source
4. **FR-Infra.6.4**: Configure auto-sync policy
5. **FR-Infra.6.5**: Setup manual sync option
6. **FR-Infra.6.6**: Configure webhook for auto-sync
7. **FR-Infra.6.7**: Setup notifications (optional)

### Non-Functional Requirements
- **NFR-Infra.6.1**: Sync interval < 5 minutes
- **NFR-Infra.6.2**: Rollback within 1 click

## Acceptance Criteria

### Given
Code pushed to Git repository

### When
ArgoCD detects changes

### Then
- [ ] Automatically syncs to K8s cluster
- [ ] Shows sync status in dashboard
- [ ] Logs sync events

### Given
Need to rollback

### When
Select previous version in ArgoCD

### Then
- [ ] Rollback to previous manifest
- [ ] Kubernetes resources updated

## Implementation Details

### ArgoCD Installation

```bash
# Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/notifications/manifests/install.yaml

# Or using Helm
helm repo add argo https://argoproj.github.io/argo-helm
helm install argocd argo/argo-cd -n argocd --create-namespace
```

### Application Manifest

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: healthlens-api
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/ie303/health-lens.git
    targetRevision: main
    path: k8s/overlays/production/api
  destination:
    server: https://kubernetes.default.svc
    namespace: healthlens
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

### GitOps Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      GITOPS FLOW                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   Developer                                                 │
│     │                                                        │
│     ▼                                                        │
│   Git Commit (k8s manifests)                                │
│     │                                                        │
│     ▼                                                        │
│   GitHub Actions (optional)                                 │
│     │                                                        │
│     ▼                                                        │
│   ArgoCD detects changes                                     │
│     │                                                        │
│     ▼                                                        │
│   Sync to K8s Cluster                                        │
│     │                                                        │
│     ▼                                                        │
│   ✓ Deployment complete                                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Project Structure

```
├── k8s/
│   ├── base/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── configmap.yaml
│   └── overlays/
│       ├── staging/
│       │   └── kustomization.yaml
│       └── production/
│           └── kustomization.yaml
```

## Dependencies

- **Pre-requisite**: K8s cluster (Infra.3)
- **Pre-requisite**: Git repository với k8s manifests

## Testing Checklist

- [ ] ArgoCD installed và accessible
- [ ] Applications created
- [ ] Auto-sync works
- [ ] Rollback works

## Notes

- Kustomize recommended cho K8s manifests
- Consider using ApplicationSet for multiple environments
- RBAC important for team access