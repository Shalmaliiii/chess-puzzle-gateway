# Testing the API Gateway

## Overview
The API Gateway is a Spring Cloud Gateway (reactive/Netty) service that routes requests to downstream microservices with JWT authentication.

## Prerequisites
- Java 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`)
- Python 3 with PyJWT (`pip3 install PyJWT`) for generating test tokens

## Running the Gateway
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd /home/ubuntu/repos/chess-puzzle-gateway
./gradlew bootRun
```
Gateway starts on port 8080. Verify with: `curl -s http://localhost:8080/actuator/health` → `{"status":"UP"}`

## Generating Test JWT Tokens
The JWT secret is configured in `src/main/resources/application.yml` under `jwt.secret`.

```python
import jwt, time
secret = 'chess-puzzle-platform-secret-key-min-256-bits-long-enough'  # from application.yml

# Valid token
valid = jwt.encode({'sub': '42', 'role': 'USER', 'iat': int(time.time()), 'exp': int(time.time()) + 3600}, secret, algorithm='HS256')

# Expired token
expired = jwt.encode({'sub': '42', 'role': 'USER', 'iat': int(time.time()) - 7200, 'exp': int(time.time()) - 3600}, secret, algorithm='HS256')

# Wrong secret token
wrong = jwt.encode({'sub': '42', 'role': 'USER', 'iat': int(time.time()), 'exp': int(time.time()) + 3600}, 'wrong-secret-key-that-is-also-256-bits-long-enough', algorithm='HS256')
```

## Testing Approach
Since downstream services (auth on 8081, puzzle on 8082, engine on 8083) may not be running during gateway-only testing, the testing strategy is:
- **Protected endpoints without valid token** → expect HTTP 401 with JSON error body
- **Protected endpoints with valid token** → expect HTTP 500 (connection refused to downstream), NOT 401. The 500 proves the JWT filter accepted the token.
- **Open endpoints** (`/api/auth/**`, `/actuator/**`) → expect HTTP 200 (actuator) or 500 (auth, if downstream not running), NOT 401

## Key Test Cases
1. Actuator health returns 200
2. Missing token on protected endpoint → 401
3. Expired token → 401
4. Wrong-secret token → 401
5. Valid token passes filter → 500 (downstream unavailable, not 401)
6. `/api/auth/login` is open → no auth required
7. `/api/auth` exact path is open
8. `/api/users/profile` is protected → 401 without token
9. Spoofed `X-User-Id`/`X-User-Role` headers are stripped
10. CORS preflight returns correct headers for allowed origins

## CORS Testing
```bash
curl -s -D- -o /dev/null -X OPTIONS \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  http://localhost:8080/api/puzzles/123
```
Expect: `Access-Control-Allow-Origin: http://localhost:3000` and methods include POST.

## Important Notes
- All testing is shell-based (curl). No browser/GUI recording needed.
- The boundary-aware open endpoint matching means `/api/auth` and `/api/auth/login` are open, but `/api/authorize` would NOT be open.
- Unroutable paths (no matching gateway route) return 404 from Spring Cloud Gateway before the global filter runs.
- The custom `GatewayErrorHandler` returns structured JSON for all error responses.

## Build & Test Commands
- Build: `./gradlew build`
- Unit tests: `./gradlew test`
- Run: `./gradlew bootRun`

## Devin Secrets Needed
None required for local testing. The JWT secret is hardcoded in `application.yml` for development.
