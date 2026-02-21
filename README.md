# Production-Grade CI/CD with Blue-Green Deployments on Kubernetes

![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Helm](https://img.shields.io/badge/Helm-0F1689?style=for-the-badge&logo=helm&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Trivy](https://img.shields.io/badge/Trivy-1904DA?style=for-the-badge&logo=aquasecurity&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Slack](https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white)

Production-grade CI/CD pipeline implementing **blue-green deployments** on Kubernetes. Each service (frontend & backend) has its own independent Jenkins pipeline that builds, tests, scans, and deploys via Helm — with automated traffic switching between blue and green environments and zero-downtime releases. Separate Slack channels for backend, frontend, and security alerts.

---

## Table of Contents

- [How It Works — Developer Workflow](#how-it-works--developer-workflow)
- [Blue-Green Deployment Strategy](#blue-green-deployment-strategy)
- [CI/CD Pipeline Stages](#cicd-pipeline-stages)
  - [Backend Pipeline](#backend-pipeline)
  - [Frontend Pipeline](#frontend-pipeline)
- [Helm Charts](#helm-charts)
- [Kubernetes Architecture](#kubernetes-architecture)
- [Docker Setup](#docker-setup)
- [Image Tagging Strategy](#image-tagging-strategy)
- [Slack Notifications](#slack-notifications)
- [Application Overview](#application-overview)
- [Project Structure](#project-structure)
- [Tools & Technologies](#tools--technologies)

---

## How It Works — Developer Workflow

A developer pushes a change to the backend. Here's exactly what happens:

**1. Push** — Developer commits a fix and pushes to the repository. Jenkins picks up the change and triggers the backend pipeline.

**2. Build & Test** — Jenkins builds a test image using the multi-stage Dockerfile's `builder` target, then runs the full Maven test suite inside the container. If tests fail, Slack sends an alert to `#backend-alerts` and the pipeline stops.

**3. Image Build** — The final production Docker image is built and tagged with both a semantic version (`v1.0.42`) and the Git commit SHA for traceability.

**4. Security Scan** — Trivy scans the container image for HIGH and CRITICAL vulnerabilities. If vulnerabilities are found, a Slack alert is sent to `#security-alerts`.

**5. Push & Clean** — The image is pushed to Docker Hub. Locally, only the 4 most recent images are kept — older ones are automatically pruned to save disk space.

**6. Determine Color** — Jenkins queries the live Kubernetes Service to find which color (blue or green) is currently receiving traffic. The pipeline targets the **opposite** color for this deployment.

**7. Helm Deploy** — The new version is deployed to the inactive color environment using `helm upgrade --install`, passing the image tag, database host, and frontend host as overrides.

**8. Health Check** — Jenkins waits for the new Deployment to reach `Ready` status via `kubectl rollout status` with a 2-minute timeout. If it fails, the old color remains active — zero impact on users.

**9. Switch Traffic** — Once healthy, Jenkins patches the Kubernetes Service selector to point to the new color. Traffic switches instantly — zero downtime.

**10. Notify** — Slack receives a success notification to `#backend-alerts` with the deployment color, image name, and build link.

> The old color stays running. If anything goes wrong, switching back is a single `kubectl patch` command.

---

## Blue-Green Deployment Strategy

This is the core of the project — **zero-downtime deployments with instant rollback**.

### How It Works

Two identical environments (blue and green) exist on the cluster. At any time, only one receives live traffic via the Kubernetes Service selector:

**Initial State** — The first deployment always goes to **blue**. The Service selector is patched to `color: blue`, and traffic routes to the blue pods.

**Subsequent Deployments** — Jenkins checks which color is active. If blue is live, it deploys to green (and vice versa). The new color runs alongside the old one. After the health check passes, Jenkins patches the Service selector to the new color — traffic switches instantly.

**Rollback** — The previous color's pods are still running. Rolling back is a single command: patch the Service selector back to the old color. No rebuild, no redeploy.

### Pipeline Logic

```groovy
// Query current active color from the live Service
def activeColor = sh(
  script: "kubectl get svc backend-service -o jsonpath='{.spec.selector.color}'",
  returnStdout: true
).trim()

// Deploy to the opposite color
def newColor = (activeColor == "blue") ? "green" : "blue"

// After successful deployment + health check:
sh "kubectl patch svc backend-service -p '{\"spec\":{\"selector\":{\"color\":\"${newColor}\"}}}'"
```

Both the backend and frontend pipelines implement this independently — each service has its own blue/green lifecycle.

---

## CI/CD Pipeline Stages

The backend and frontend each have their own `Jenkinsfile` with independent pipelines. Both follow the same pattern but with service-specific tooling.

### Backend Pipeline

| # | Stage | What Happens |
|---|-------|-------------|
| 1 | **Set Image Tags** | Generates semantic version `v<MAJOR>.<MINOR>.<BUILD>` + Git commit SHA tag |
| 2 | **Build** | Builds test image using Dockerfile's `builder` stage (Maven + JDK 17) |
| 3 | **Test** | Runs `mvn test` inside the builder container |
| 4 | **Image Build** | Builds final production image (JRE 17 Alpine only, no build tools) |
| 5 | **Image Scanning** | Trivy scans for HIGH/CRITICAL CVEs |
| 6 | **Image Upload & Clean** | Pushes to Docker Hub, prunes old local images (keeps latest 4) |
| 7 | **Determine Color** | Queries K8s Service for active color, targets the opposite |
| 8 | **Apply Base Manifests** | First deployment only — creates Service with NodePort |
| 9 | **Helm Deploy** | `helm upgrade --install` with image tag, DB host, frontend host overrides |
| 10 | **Check Deployment Status** | `kubectl rollout status` with 2-minute timeout |
| 11 | **Switch Traffic** | Patches Service selector to new color |

### Frontend Pipeline

| # | Stage | What Happens |
|---|-------|-------------|
| 1 | **Set Image Tags** | Generates semantic version + Git commit SHA tag |
| 2 | **Build** | Builds test image using Dockerfile's `builder` stage (Node.js) |
| 3 | **Test** | Runs `npm test -- --watchAll=false` inside the builder container |
| 4 | **Image Build** | Builds final production image (Nginx Alpine serving static React build) |
| 5 | **Image Scanning** | Trivy scans with `--exit-code 1` — **fails the pipeline** on HIGH/CRITICAL CVEs |
| 6 | **Image Upload & Clean** | Pushes to Docker Hub, prunes old local images (keeps latest 4) |
| 7 | **Determine Color** | Queries K8s Service for active color, targets the opposite |
| 8 | **Apply Base Manifests** | First deployment only — creates Service with NodePort |
| 9 | **Helm Deploy** | `helm upgrade --install` with image tag, backend host/port overrides |
| 10 | **Check Deployment Status** | `kubectl rollout status` with 2-minute timeout |
| 11 | **Switch Traffic** | Patches Service selector to new color |

---

## Helm Charts

Each service has its own Helm chart under `helm-charts/`. The charts are designed for blue-green deployments — every resource is **color-scoped**.

### Backend Chart

| Template | Kind | Details |
|----------|------|---------|
| `backend_depl.yaml` | Deployment | Named `inventory-backend-<color>`, image tag and all env vars injected via values |
| `configmap.yaml` | ConfigMap | Named `inventory-app-config-backend-<color>`, holds DB_HOST, DB_PORT, FRONTEND_HOST, FRONTEND_PORT |

Environment variables are sourced from:
- **ConfigMap** — DB_HOST, DB_PORT, FRONTEND_HOST, FRONTEND_PORT (non-sensitive)
- **Secret** — DB_USER, DB_PASSWORD (via `inventory-db-secret`, managed separately)

### Frontend Chart

| Template | Kind | Details |
|----------|------|---------|
| `frontend_depl.yaml` | Deployment | Named `inventory-frontend-<color>`, backend URL injected via ConfigMap |
| `configmap.yaml` | ConfigMap | Named `inventory-app-config-frontend-<color>`, holds backend_host and backend_port |

### Values Override at Deploy Time

Jenkins passes runtime values during `helm upgrade --install`:

```bash
helm upgrade --install inventory-backend-green ./helm-charts/backend \
  --set image.tag=v1.0.42 \
  --set db_host=$INVENTORY_DB_HOST \
  --set color=green \
  --set activeColor=blue \
  --set frontend_host=$FRONTEND_HOST \
  --set frontend_port=$FRONTEND_PORT
```

---

## Kubernetes Architecture

### Base Manifests (First Deployment Only)

Applied once during the initial deployment to create the Services:

| Manifest | Kind | Details |
|----------|------|---------|
| `backend_svc.yaml` | Service (NodePort) | Port 8080 → NodePort 30081, selector `app: inventory-backend` + `color: <active>` |
| `frontend_svc.yaml` | Service (NodePort) | Port 80 → NodePort 30080, selector `app: inventory-frontend` + `color: <active>` |

The `color` selector on the Service is what enables blue-green traffic switching — Jenkins patches this to point to the active color.

### Runtime State (After Multiple Deployments)

After the pipeline has run a few times, the cluster looks like this:

| Resource | Name | Status |
|----------|------|--------|
| Deployment | `inventory-backend-blue` | Running (previous version) |
| Deployment | `inventory-backend-green` | Running (current version, receiving traffic) |
| Service | `backend-service` | Selector: `color: green` |
| Deployment | `inventory-frontend-blue` | Running (previous version) |
| Deployment | `inventory-frontend-green` | Running (current version, receiving traffic) |
| Service | `frontend-service` | Selector: `color: green` |

---

## Docker Setup

### Backend — Multi-stage Build

```dockerfile
# Stage 1: Build & test (Maven + JDK 17)
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
COPY pom.xml ./
RUN mvn dependency:go-offline -B     # Cache dependencies
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production (JRE only)
FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

The `builder` stage is also used independently during CI — Jenkins builds up to the `builder` target and runs `mvn test` inside it before building the final image.

### Frontend — Multi-stage Build with Runtime Config

```dockerfile
# Stage 1: Build React app
FROM node:alpine AS builder
RUN npm ci && npm run build

# Stage 2: Serve via Nginx
FROM nginx:stable-alpine
COPY --from=builder /app/build/ /usr/share/nginx/html/
COPY docker-entrypoint.sh /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
```

The custom `docker-entrypoint.sh` generates a `config.js` file at container startup that injects the backend URL as a runtime variable — this allows the same image to work across environments without rebuilding:

```bash
cat << EOF > /usr/share/nginx/html/config.js
window.__BACKEND_URL__ = "http://${BACKEND_HOST}:${BACKEND_PORT}";
EOF
```

---

## Image Tagging Strategy

Every build produces **two tags** per image:

| Tag | Format | Purpose |
|-----|--------|---------|
| **Semantic Version** | `v<MAJOR>.<MINOR>.<BUILD_NUMBER>` | Human-readable, tracks releases |
| **Git Commit SHA** | Full SHA hash | Exact traceability to source code |

Major and minor versions are configured as Jenkins environment variables (`INVENTORY_BACKEND_MAJOR`, `INVENTORY_BACKEND_MINOR`), and the build number is auto-incremented.

### Local Image Cleanup

After each push, the pipeline prunes old images — keeping only the **4 most recent** to prevent disk bloat on the Jenkins agent:

```bash
docker images ${IMAGE_REPOSITORY} --format "{{.ID}} {{.CreatedAt}}" \
  | sort -r -k2 | awk 'NR>4 {print $1}' | sort -u | xargs -r docker rmi -f
```

---

## Slack Notifications

Alerts are routed to **dedicated channels** based on service and severity:

| Channel | Triggers |
|---------|----------|
| `#backend-alerts` | Backend build failures, test failures, deployment failures, successful deployments |
| `#frontend-alerts` | Frontend build failures, test failures, deployment failures, successful deployments |
| `#security-alerts` | Trivy vulnerability scan findings (both services) |

Every notification includes the build number, image name, build URL link, and timestamp.

---

## Application Overview

The underlying application is an **Inventory Management System** — a full-stack web app used to demonstrate the CI/CD pipeline.

| Tier | Technology | Details |
|------|-----------|---------|
| **Frontend** | React 18, React Bootstrap, Axios | SPA served via Nginx, runtime backend URL injection |
| **Backend** | Spring Boot 3.2, Java 17, Spring Security, JPA | REST API with JWT auth, BCrypt passwords, PostgreSQL |
| **Database** | PostgreSQL | Schema includes users, products, orders, order_items, inventory_history |

### Features
- JWT-based authentication with Spring Security
- Product management (CRUD with SKU tracking)
- Order management with order items
- Inventory history / audit trail
- Role-based access (per-user product/order scoping)

---

## Project Structure

```
prod-grade-cicd-k8s/
├── backend/
│   ├── Jenkinsfile              # 11-stage CI/CD pipeline with blue-green deployment
│   ├── Dockerfile               # Multi-stage: Maven builder → JRE 17 Alpine runner
│   ├── pom.xml                  # Spring Boot 3.2, PostgreSQL, JWT, Spring Security
│   └── src/
│
├── frontend/
│   ├── Jenkinsfile              # 11-stage CI/CD pipeline with blue-green deployment
│   ├── Dockerfile               # Multi-stage: Node builder → Nginx Alpine
│   ├── docker-entrypoint.sh     # Runtime backend URL injection via config.js
│   ├── package.json             # React 18, React Bootstrap, Axios, React Router v6
│   └── src/
│
├── helm-charts/
│   ├── backend/
│   │   ├── Chart.yaml
│   │   ├── values.yaml          # Image repo/tag, DB host, frontend host, color
│   │   └── templates/
│   │       ├── backend_depl.yaml    # Color-scoped Deployment
│   │       └── configmap.yaml       # Color-scoped ConfigMap
│   └── frontend/
│       ├── Chart.yaml
│       ├── values.yaml          # Image repo/tag, backend host/port, color
│       └── templates/
│           ├── frontend_depl.yaml   # Color-scoped Deployment
│           └── configmap.yaml       # Color-scoped ConfigMap
│
├── base-k8s/
│   ├── backend/
│   │   └── backend_svc.yaml     # NodePort Service (30081) with color selector
│   └── frontend/
│       └── frontend_svc.yaml    # NodePort Service (30080) with color selector
│
├── db-setup/
│   ├── init.sql                 # Create database + user
│   └── schema.sql               # Grant schema permissions
│
└── runLocally.md                # Local development setup guide
```

---

## Tools & Technologies

| Category | Tools |
|----------|-------|
| **CI/CD** | Jenkins (Declarative Pipelines, per-service) |
| **Deployment Strategy** | Blue-Green via Kubernetes Service selector patching |
| **Container Orchestration** | Kubernetes, Helm (color-scoped charts) |
| **Containerization** | Docker (multi-stage builds), Docker Hub |
| **Security Scanning** | Trivy (container image vulnerability scanning, HIGH/CRITICAL) |
| **Notifications** | Slack (dedicated channels: backend-alerts, frontend-alerts, security-alerts) |
| **Backend** | Spring Boot 3.2, Java 17, Spring Security, JPA/Hibernate, JWT, BCrypt |
| **Frontend** | React 18, React Bootstrap, Axios, React Router v6, Nginx |
| **Database** | PostgreSQL (env-based config, runtime secrets via K8s Secrets) |
| **Image Management** | Semantic versioning + Git SHA tags, automated local image pruning |