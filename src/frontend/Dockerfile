# syntax=docker/dockerfile:1
# check=skip=SecretsUsedInArgOrEnv
FROM node:22.14-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY . .
# Vite inlines VITE_* into the SPA bundle at build time, so the auth provider
# and Entra config must be present for `npm run build`. Passed via --build-arg
# (see scripts/minikube-deploy.sh); a plain `docker build` defaults to the dev
# auth module. These VITE_* values are NOT secrets — the provider name, tenant
# id, and client ids are inlined into the (public) client bundle by design —
# hence the top-of-file `check=skip=SecretsUsedInArgOrEnv` that silences
# BuildKit's false-positive lint on the "AUTH"-containing name.
ARG VITE_AUTH_PROVIDER=dev
ARG VITE_ENTRA_TENANT_ID=
ARG VITE_ENTRA_SPA_CLIENT_ID=
ARG VITE_ENTRA_API_CLIENT_ID=
ARG VITE_ENTRA_REDIRECT_URI=
ENV VITE_AUTH_PROVIDER=$VITE_AUTH_PROVIDER \
    VITE_ENTRA_TENANT_ID=$VITE_ENTRA_TENANT_ID \
    VITE_ENTRA_SPA_CLIENT_ID=$VITE_ENTRA_SPA_CLIENT_ID \
    VITE_ENTRA_API_CLIENT_ID=$VITE_ENTRA_API_CLIENT_ID \
    VITE_ENTRA_REDIRECT_URI=$VITE_ENTRA_REDIRECT_URI
RUN npm run build

FROM nginx:1.27-alpine
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
