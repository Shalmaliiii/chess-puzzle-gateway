# Chess Puzzle Platform — API Gateway

Spring Cloud Gateway service for the Chess Puzzle Platform. Routes requests to downstream microservices with JWT authentication, CORS configuration, and centralized error handling.

## Tech Stack

- **Java 21** (LTS)
- **Spring Boot 3.4.x**
- **Spring Cloud Gateway** (reactive/Netty, Spring Cloud 2024.0.x)
- **JJWT** (io.jsonwebtoken) for JWT validation
- **Lombok**
- **Gradle** (Groovy DSL)

## Architecture

```
Client → API Gateway (8080) → Auth/User Service (8081)
                             → Puzzle Service (8082)
                             → Engine Service (8083)
```

## Routes

| Route ID        | Path            | Upstream Service             |
|-----------------|-----------------|------------------------------|
| auth-service    | /api/auth/**    | http://localhost:8081         |
| user-service    | /api/users/**   | http://localhost:8081         |
| puzzle-service  | /api/puzzles/** | http://localhost:8082         |
| engine-service  | /api/engine/**  | http://localhost:8083         |

## Authentication

- **Open endpoints** (no auth required): `/api/auth/**`, `/actuator/**`
- **Protected endpoints**: All other `/api/**` routes require a valid JWT in the `Authorization: Bearer <token>` header.
- On valid JWT, the gateway adds `X-User-Id` and `X-User-Role` headers to downstream requests.

## Configuration

Configuration is in `src/main/resources/application.yml`. The `application-docker.yml` profile overrides service URIs for Docker/container networking.

### Environment Variables

| Variable       | Description            | Default                                                      |
|----------------|------------------------|--------------------------------------------------------------|
| `server.port`  | Gateway listen port    | 8080                                                         |
| `jwt.secret`   | JWT signing secret key | `chess-puzzle-platform-secret-key-min-256-bits-long-enough`  |

## Running Locally

### Prerequisites
- Java 21

### Build & Run

```bash
./gradlew build
./gradlew bootRun
```

The gateway starts on `http://localhost:8080`.

### Run Tests

```bash
./gradlew test
```

## Docker

### Build Image

```bash
docker build -t chess-puzzle-gateway .
```

### Run Container

```bash
docker run -p 8080:8080 chess-puzzle-gateway
```

For Docker Compose / container networking, activate the `docker` profile:

```bash
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker chess-puzzle-gateway
```

## CORS

Configured for frontend origins:
- `http://localhost:3000`
- `http://localhost:5173`

Allowed methods: GET, POST, PUT, DELETE, OPTIONS.

## Error Handling

The gateway returns structured JSON error responses:

```json
{
  "error": "Unauthorized",
  "status": 401,
  "message": "Missing or invalid Authorization header",
  "path": "/api/puzzles/123"
}
```
