# llm-proxy

Proxy LLM configurable via UI admin, avec authentification des utilisateurs et stockage asynchrone des metriques.

## Stack choisi

- Backend: Spring Boot 3.4 (Web MVC + WebClient + Security + JPA)
- DB: H2 file (`backend/data/llm-proxy-db`)
- Frontend: Angular 19 (admin console)

Ce choix est pragmatique pour ton contexte actuel:
- tres rapide a bootstraper
- OAuth2/OIDC bien supporte en resource server
- H2 simple pour prototypage
- facile a migrer vers Postgres ensuite

## Architecture

1. L'admin se connecte et configure les routes LLM en DB:
   - exemple: `/app1/llm` -> `https://my-llm-api`
   - auth sortante configurable (`NONE` ou `BEARER`)
2. Un user se connecte (OIDC/basic) et genere un token API personnel.
3. Le token est stocke en hash (jamais en clair), mappe a `issuer + sub`.
4. Le user appelle le proxy avec `Authorization: Bearer <token-personnel>`.
5. Le backend trouve la meilleure route active (prefix le plus long) et proxifie l'appel.
6. Les metriques sont enregistrees en async puis exposees au dashboard admin.

## Arborescence

- `backend`: API Spring Boot
- `frontend`: UI Angular admin

## Backend

### Endpoints admin (ROLE_ADMIN)

- `GET /api/admin/routes`
- `POST /api/admin/routes`
- `PUT /api/admin/routes/{id}`
- `DELETE /api/admin/routes/{id}`
- `GET /api/admin/metrics/dashboard`
- `GET /api/admin/metrics/recent?limit=200`

### Endpoints user token (user authentifie)

- `GET /api/me/tokens`
- `POST /api/me/tokens`
- `PATCH /api/me/tokens/{id}/expiry`
- `DELETE /api/me/tokens/{id}`

Politique d'expiration:
- `NEVER`
- `P30D` (1 mois)
- `P365D` (1 an)

### Endpoint auth session

- `GET /api/auth/me` (retourne user + roles + type auth)

### Endpoint proxy

- Catch-all sur les routes non reservees.
- Exemple:
  - route config: `/app1/llm` -> `https://my-llm-api`
  - appel entrant: `POST /app1/llm/chat/completions`
  - appel sortant: `POST https://my-llm-api/chat/completions`

### Securite

Par defaut (`app.security.oauth2.enabled=false`):
- Basic auth local:
  - `admin / admin123` (ROLE_ADMIN)
  - `user / user123` (ROLE_USER)

Mode OAuth2/OIDC:
- mettre `app.security.oauth2.enabled=true`
- configurer `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- `/api/admin/**` demande `ROLE_ADMIN`
- `/api/auth/**` et `/api/me/**` demandent `ROLE_ADMIN` ou `ROLE_USER`
- le proxy accepte les tokens API personnels (PAT) en bearer

## Frontend

L'UI Angular permet:
- page login claire (`login/password` ou `OIDC token`)
- navigation par role:
  - ADMIN: dashboard metriques + menu routes LLM
  - USER: menu gestion des tokens uniquement
- dashboard admin: daily/monthly activities, top consumers, top routes, trafic recent
- gestion des tokens user avec duree configurable

## Lancer en local

### 1) Backend

```bash
cd backend
mvn spring-boot:run
```

API sur `http://localhost:8085`.

### 2) Frontend

```bash
cd frontend
nvm use
npm install
npm start
```

UI sur `http://localhost:4200`.

## Notes importantes

- CORS autorise `http://localhost:4200` pour le dev.
- Angular 19: utiliser Node LTS pair (22 recommande). Node 23 peut casser le build.
- Les tokens user sont stockes en hash SHA-256.
- Les tokens sortants (vers LLM cible) sont stockes en clair dans H2 pour l'instant (prototype).
- En production:
  - chiffrer les secrets en DB
  - migrer H2 -> Postgres
  - ajouter quotas/rate-limit
  - ajouter agregats metriques (par user/jour/route)
