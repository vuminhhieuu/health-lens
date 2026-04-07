---
story_id: "infra-5"
epic: "infra"
story_key: "infra-5-monitoring-prometheus-grafana"
title: "Monitoring với Prometheus + Grafana"
status: "ready-for-dev"
priority: "P6"
created_date: "2026-04-01"
input_artifacts: ["prd.md", "architecture.md", "infra-3-production-k8s.md"]
---

# Story: Monitoring với Prometheus + Grafana

## User Story Statement

As a DevOps engineer,
I want có monitoring và alerting system,
So that có thể track performance, detect issues early, và maintain production health.

## Business Value

- Proactive issue detection
- Performance visibility
- Capacity planning data
- Incident response faster
- SLA compliance proof

## Technical Context

### Target Components
- **Metrics**: Prometheus
- **Visualization**: Grafana
- **Logs**: Loki (optional)
- **Alerts**: AlertManager

### Application Metrics Needed
- API response time
- Error rates
- Request count
- CPU/Memory usage
- Database connections

## Requirements

### Functional Requirements
1. **FR-Infra.5.1**: Install Prometheus on K8s
2. **FR-Infra.5.2**: Configure service monitors cho API và Web
3. **FR-Infra.5.3**: Install Grafana và configure dashboards
4. **FR-Infra.5.4**: Setup AlertManager với alerts
5. **FR-Infra.5.5**: Configure alerts for critical metrics
6. **FR-Infra.5.6**: Setup notification channels (Slack/Email)
7. **FR-Infra.5.7**: Create default dashboards

### Non-Functional Requirements
- **NFR-Infra.5.1**: Metrics retention: 15 days
- **NFR-Infra.5.2**: Dashboard load time < 2 seconds
- **NFR-Infra.5.3**: Alert delivery < 1 minute

## Acceptance Criteria

### Given
Monitoring installed

### When
Access Grafana

### Then
- [ ] Dashboards visible
- [ ] Metrics updating
- [ ] Historical data available

### Given
Service has issues

### When
Metrics exceed threshold

### Then
- [ ] Alert triggered
- [ ] Notification sent
- [ ] Dashboard shows issue

## Implementation Details

### K8s Installation (Helm)

```bash
# Add repos
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts

# Install Prometheus
helm install prometheus prometheus-community/prometheus \
  --namespace monitoring --create-namespace

# Install Grafana
helm install grafana grafana/grafana \
  --namespace monitoring \
  --set admin.password=admin
```

### Service Monitor Example

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: healthlens-api
spec:
  selector:
    matchLabels:
      app: healthlens-api
  endpoints:
  - port: http
    path: /actuator/prometheus
```

### Default Dashboards

1. **API Dashboard**
   - Request rate
   - Response time (p50, p95, p99)
   - Error rate
   - JVM metrics

2. **Web Dashboard**
   - Page views
   - API call latency
   - Error count
   - User sessions

3. **System Dashboard**
   - CPU usage
   - Memory usage
   - Disk usage
   - Network traffic

### Alert Rules

```yaml
groups:
- name: healthlens
  rules:
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
```

## Dependencies

- **Pre-requisite**: K8s cluster (Infra.3)
- **Pre-requisite**: API exposed /actuator/prometheus endpoint

## Testing Checklist

- [ ] Prometheus collecting metrics
- [ ] Grafana accessible
- [ ] Dashboards working
- [ ] Alerts triggering
- [ ] Notifications sent

## Notes

- For staging: can use free Grafana Cloud
- Start with basic metrics, expand as needed
- Consider cost of long-term storage