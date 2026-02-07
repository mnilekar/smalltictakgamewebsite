# smalltictakgamewebsite
Tic-Tac-Toe microservices project (Java + Oracle + HTML/CSS).

## Quick Tunnels (no domain)

Run each service locally, then start Cloudflare “Quick Tunnels” to get public URLs. Use those URLs to configure CORS on the APIs and to let the UI discover the API bases via `/config.json`.

**Start apps (3 terminals):**
```
./mvnw -pl auth-service spring-boot:run
```
```
./mvnw -pl game-service spring-boot:run
```
```
./mvnw -pl web-ui spring-boot:run
```

**Start tunnels (3 terminals):**
```
cloudflared tunnel --url http://localhost:8081
```
```
cloudflared tunnel --url http://localhost:8091
```
```
cloudflared tunnel --url http://localhost:8080
```

Each tunnel prints a public URL. Set those URLs as environment variables and restart the apps so CORS and `/config.json` are correct:

```
export AUTH_BASE="https://<auth-tunnel>.trycloudflare.com"
export GAME_BASE="https://<game-tunnel>.trycloudflare.com"
export WS_BASE="wss://<game-tunnel>.trycloudflare.com/ws"
export CORS_ALLOWED_ORIGINS="https://<ui-tunnel>.trycloudflare.com"
```

Then restart the auth-service and game-service with `CORS_ALLOWED_ORIGINS` set, and restart web-ui with `AUTH_BASE`, `GAME_BASE`, and `WS_BASE` set. The UI will fetch `/config.json` at runtime and use those public URLs automatically.
