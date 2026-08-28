# syntax=docker/dockerfile:1
FROM node:22.14-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY . .
# No VITE_* build args (task-162). The auth coordinates used to be inlined into
# the bundle here, which made THIS IMAGE the configuration: staging and
# production needed separately built images and an image could not be promoted
# between stages. They are now read at runtime from /config.json, served from a
# ConfigMap — so one image serves every stage.
RUN npm run build

FROM nginx:1.27-alpine
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
