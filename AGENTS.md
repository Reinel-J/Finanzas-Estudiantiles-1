# AGENTS.md — FinanzasApp (Finanzas Estudiantiles)

## Project overview

Student finance tracker: plain HTML/JS frontend calling a Spring Boot REST API backed by MySQL.

## Directory layout

| Path | Purpose |
|------|---------|
| `frontend/` | Static HTML/CSS/JS pages (no build tool, served directly by browser/Live Server) |
| `frontend/js/api.js` | Central API client — all backend calls go through this file |
| `finanzas-backend/finanzas-backend/` | Spring Boot 3.2.0 app (Java 17, Maven) |
| `finanzas-backend/finanzas-backend/src/main/resources/schema.sql` | MySQL DDL — run manually to create DB |
| `backend/` | **Unused** — do not modify |

## Ports and services

| Service | Port |
|---------|------|
| Frontend | 8080 (served by Live Server or browser `file://`) |
| Backend (Spring Boot) | 8081 |
| MySQL | 3307 |

**DB name**: `finanzas_estudiantiles` | **user**: `root` | **pass**: `1234`

## Commands

### Docker (full stack: MySQL + backend + frontend)
```bash
docker compose up -d --build      # frontend :8080, backend :8081, MySQL :3307
docker compose down -v            # stop and wipe DB volume
```
Ports/passwords overridable via `.env` (see `.env.example`). Backend gets DB settings from `SPRING_DATASOURCE_*` env vars in `docker-compose.yml`.

### Start backend
```bash
cd finanzas-backend/finanzas-backend
./mvnw spring-boot:run
```

### Run tests
```bash
cd finanzas-backend/finanzas-backend
./mvnw test
```

### Build JAR
```bash
cd finanzas-backend/finanzas-backend
./mvnw package
```

### Frontend
Open `frontend/index.html` in a browser. No build step. Use VS Code Live Server on port 8080 if needed.

## Setup prerequisites

1. MySQL must be running on port **3307** (non-standard)
2. Create DB via `schema.sql` before first run: `mysql -u root -p1234 -P 3307 < finanzas-backend/finanzas-backend/src/main/resources/schema.sql`
3. Hibernate `ddl-auto=update` will create/modify tables on startup, but the DB must exist first

## Architecture notes

- **Auth**: Application handles login/registration manually in `UsuarioService`. Spring Security is configured to `permitAll()` — it does NOT enforce authentication.
- **Session**: Frontend uses `sessionStorage` (cleared on tab close), NOT `localStorage`.
- **CORS**: Enabled via `SecurityConfig.java` — required because frontend (8080) and backend (8081) are different origins.
- **Passwords**: Stored plain-text (no `PasswordEncoder` bean). `@JsonProperty(access = WRITE_ONLY)` on `Usuario.contrasena` prevents leaking passwords in JSON responses.
- **Lombok**: Used for boilerplate reduction. IDEs need Lombok plugin; Maven compiles with annotation processor.
- **JSON of entities**: `usuario` relations and `MetaAhorro.historialPrecios` / `HistorialPrecio.meta` are `@JsonIgnore` (avoids Hibernate lazy-proxy 500s and infinite recursion). Keep bidirectional relations out of Lombok `toString`/`equals` with `@ToString.Exclude` / `@EqualsAndHashCode.Exclude`.
- **Create endpoints ignore client `id`**: services call `setId(null)` before `save()`; otherwise JPA merges and overwrites an existing row (this allowed account takeover via `/usuarios/registro`).
- **Errors**: `GlobalExceptionHandler` returns `{ "error": "..." }` for any uncaught exception — the shape `fetchAPI()` expects.
- **Currency**: `MonedaService` uses the keyless `open.er-api.com` endpoint unless `moneda.api-key` / `MONEDA_API_KEY` is set. Rates cached 1 h.
- **Timezone**: Docker backend runs with `TZ=America/Bogota` so `LocalDate.now()` matches the user's day.

## API endpoints (base: `http://localhost:8081/api`)

All endpoints are documented in `frontend/js/api.js`. Key patterns:
- `POST /usuarios/registro` — create user
- `POST /usuarios/login` — authenticate (returns user object)
- `/ingresos/{usuarioId}` — GET list, POST create, DELETE by id, GET `/total`
- `/gastos/{usuarioId}` — same pattern as ingresos
- `/metas/{usuarioId}` — savings goals CRUD, PUT `/{id}/abonar` to add funds
- `/semestre/{usuarioId}` — semester config PUT, POST `/finalizar` to end semester, GET `/historial`
- `/moneda/convertir` — currency conversion POST
- `/reportes/{usuarioId}` — save/get/delete report snapshots

## Conventions

- Field naming: backend uses `monto`, `descripcion`, `fecha`, `categoria` — frontend forms must match these exactly
- Dates are `YYYY-MM-DD` strings
- Amounts are `DOUBLE` in both Java and MySQL
- `verificarSesion()` in `api.js` auto-redirects unauthenticated users to `index.html`
- New users are flagged with `primeraVez=true` in sessionStorage and redirected to `semestre.html` for initial setup
