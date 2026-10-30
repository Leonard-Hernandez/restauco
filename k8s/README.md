# Restauco on Kubernetes

Same stack as `docker-compose.yml`: postgres (restauco + authco databases), authco, the restauco API
and the restauco-app SPA, behind one ingress (`/restauco-api`, `/authco`, `/`).

1. Build and push the images (or load them into a local cluster):
   `docker compose build` -> `leonard20/restauco`, `leonard20/restauco-app`, `leonard20/authco`.
2. Fill `config/secret.yml` with real values (base64). Google OAuth redirect:
   `<origin>/authco/login/oauth2/code/google`.
3. Replace `http://localhost` in `config/configmap.yml` with the public origin.
4. `kubectl apply -k k8s/`
5. Open `<origin>/panel` with the owner email and invite the team (Equipo).

The API runs 1 replica on purpose: live updates (SSE) fan out in memory. The ingress disables
proxy buffering and allows 1 h reads so the streams stay open.

Render without applying: `kubectl kustomize k8s/`.

## On the shared platform

`../k8s-shared` runs the same stack on the shared Postgres and authco of
[`../../platform/k8s`](../../platform/README.md) (namespace `platform`). It drops its own
`postgres` and `authco` and points those service names at the platform with ExternalName
services. It is served on `http://restauco.localhost`. The standalone `k8s/` keeps working
as before.

```sh
kubectl apply -k ../platform/k8s    # once, from the code directory
kubectl apply -k k8s-shared
```
