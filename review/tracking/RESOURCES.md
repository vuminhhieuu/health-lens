# 🛠️ HealthLens Refactor — Resources & Cheatsheet

> Lookup nhanh: deps, doc links, snippets, commands.

---

## 📦 Dependencies (theo phase)

### Phase 1.2 — ShedLock
```kotlin
implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")
```

### Phase 1.3 — Apache Tika
```kotlin
implementation("org.apache.tika:tika-core:2.9.2")
```

### Phase 2 — Tooling
```kotlin
// Spotless
plugins { id("com.diffplug.spotless") version "6.25.0" }
spotless {
    java {
        googleJavaFormat("1.22.0")
        target("src/**/*.java")
    }
}
```
```json
// package.json (root)
{ "devDependencies": {
    "prettier": "^3.3.0",
    "husky": "^9.1.0",
    "lint-staged": "^15.2.0"
}}
```

### Phase 4 — MapStruct
```kotlin
implementation("org.mapstruct:mapstruct:1.6.0")
annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0")
```

### Phase 8 — Observability
```kotlin
implementation("io.micrometer:micrometer-registry-prometheus")
implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.14.0")
```

---

## 🔗 Documentation Links

- [Spring Security Headers DSL](https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html)
- [ShedLock](https://github.com/lukas-krecan/ShedLock)
- [Hibernate Soft Delete](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#soft-delete)
- [Apache Tika MIME](https://tika.apache.org/2.9.0/detection.html)
- [Spring AI Chat Client](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [OWASP Secure Headers](https://owasp.org/www-project-secure-headers/)
- [Anthropic Prompt Injection](https://docs.claude.com/en/docs/test-and-evaluate/strengthen-guardrails/mitigate-jailbreaks)
- [RFC 7807 ProblemDetail](https://www.rfc-editor.org/rfc/rfc7807)
- [GDPR Article 20](https://gdpr-info.eu/art-20-gdpr/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Spotless Gradle](https://github.com/diffplug/spotless)
- [Flyway Best Practices](https://documentation.red-gate.com/fd/migrations-184127470.html)

---

## 💻 Commands

### Branch + PR
```bash
git checkout -b phase/00-safety-net main
git fetch origin && git rebase origin/main
git push -u origin phase/00-safety-net
gh pr create --title "chore(safety): runtime + cleanup"
```

### Test
```bash
./gradlew :apps:api:test
pnpm -F @healthlens/web test
cd services/ocr-service && pytest
./gradlew :apps:api:jacocoTestReport
```

### Smoke tests
```bash
# HTTP headers
curl -I https://api.healthlens.vn/actuator/health | grep -E "Strict-Transport|Content-Security|X-Frame"

# Multi-pod scheduler
docker compose -f compose.dev.yml up --scale api=2
# Wait, verify single fire

# Flyway
./gradlew :apps:api:flywayValidate
./gradlew :apps:api:flywayMigrate
```

---

## 📋 Code Snippets

### Phase 0.A.1 — HTTP Security Headers
```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; img-src 'self' data: https://*.healthlens.vn; " +
        "script-src 'self'; style-src 'self' 'unsafe-inline'; " +
        "connect-src 'self' https://api.healthlens.vn; " +
        "frame-ancestors 'none'; form-action 'self'"))
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .referrerPolicy(rp -> rp.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=(), payment=()"))
);
```

### Phase 0.A.2 — Soft Delete Annotation
```java
@Entity
@Table(name = "health_records")
@SQLDelete(sql = "UPDATE health_records SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class HealthRecord { ... }
```

### Phase 1.1 — PromptSanitizer
```java
public final class PromptSanitizer {
    private static final Pattern INSTRUCTION_BREAK = Pattern.compile(
        "(?i)(ignore\\s+(previous|all)|new\\s+instruction|system\\s*:|<\\|.*\\|>|---\\s*end\\s*---)"
    );
    private PromptSanitizer() {}
    public static String sanitize(String userInput) {
        if (userInput == null) return "";
        return INSTRUCTION_BREAK.matcher(userInput.trim()).replaceAll("[REDACTED]");
    }
}
```

### Phase 1.2 — @SchedulerLock
```java
@Scheduled(cron = "0 0 * * * *")
@SchedulerLock(name = "follow-up-reminder", lockAtMostFor = "55m", lockAtLeastFor = "30s")
public void sendDueReminderEmails() { ... }
```

### Phase 1.3 — Apache Tika
```java
@Component
public class FileTypeVerifier {
    private static final Tika TIKA = new Tika();
    private static final Set<String> ALLOWED = Set.of(
        "image/jpeg", "image/png", "application/pdf"
    );
    public void verify(byte[] header) {
        String detected = TIKA.detect(header);
        if (!ALLOWED.contains(detected)) {
            throw new InvalidFileTypeException(detected);
        }
    }
}
```

### Phase 3.1 — HealthRecordStatus enum
```java
public enum HealthRecordStatus {
    PROCESSING("processing"),
    REVIEW_REQUIRED("review_required"),
    DONE("done"),
    OCR_FAILED("ocr_failed");

    private final String dbValue;
    private static final Map<HealthRecordStatus, Set<HealthRecordStatus>> TRANSITIONS = Map.of(
        PROCESSING,      Set.of(REVIEW_REQUIRED, OCR_FAILED),
        REVIEW_REQUIRED, Set.of(DONE, OCR_FAILED),
        OCR_FAILED,      Set.of(PROCESSING, DONE),
        DONE,            Set.of()
    );

    HealthRecordStatus(String dbValue) { this.dbValue = dbValue; }
    public String dbValue() { return dbValue; }
    public boolean canTransitionTo(HealthRecordStatus t) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(t);
    }
    public static HealthRecordStatus fromDb(String v) {
        for (var s : values()) if (s.dbValue.equals(v)) return s;
        throw new IllegalArgumentException("Unknown status: " + v);
    }
}
```

---

## 🧪 Verification Templates

### After Phase 0
```bash
# Security headers
curl -sI http://localhost:8080/actuator/health | \
  grep -E "Strict-Transport-Security|Content-Security-Policy|X-Frame-Options"
# Expect: 6 headers

# Soft delete leak
./gradlew :apps:api:test --tests "*SoftDelete*"

# Redis cache purge after deletion
redis-cli KEYS "llm:explanation:$DELETED_USER_ID:*"
# Expect: empty
```

### After Phase 1
```bash
# Prompt injection blocked
./gradlew :apps:api:test --tests "*PromptSanitizer*"

# ShedLock active
psql $DATABASE_URL -c "SELECT * FROM shedlock"

# Magic-byte rejection
curl -X POST localhost:8080/health-records/confirm-upload \
  -F "file=@malicious.html;type=image/jpeg"
# Expect: 400
```

---

## 🆘 Troubleshooting

### "spotlessCheck failed"
```bash
./gradlew spotlessApply && git diff
```

### "Husky hook not running"
```bash
pnpm install && npx husky init
```

### "ShedLock not preventing duplicate runs"
- Check `lockAtMostFor` > job execution time
- Verify single `@EnableSchedulerLock`
- Use server-side `now()` not pod time
