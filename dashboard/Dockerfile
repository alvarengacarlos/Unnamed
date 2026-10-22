# Builds the dashboard and serves it with Caddy. Vite writes the VITE_* variables into the build, so they are build
# arguments, not variables of the container:
#   docker build --build-arg VITE_API_BASE_URL=... --build-arg VITE_OIDC_AUTHORITY=... \
#       --build-arg VITE_OIDC_CLIENT_ID=... .
FROM node:24-alpine AS build
WORKDIR /dashboard
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL
ARG VITE_OIDC_AUTHORITY
ARG VITE_OIDC_CLIENT_ID
# A missing one would only show in the browser, as requests to "undefined". The URLs go without the / at the end, as
# the app adds the paths to them and the Content-Security-Policy adds the / that makes them match those paths.
RUN for name in VITE_API_BASE_URL VITE_OIDC_AUTHORITY VITE_OIDC_CLIENT_ID; do \
        eval "value=\${$name}"; \
        [ -n "$value" ] || { echo "Missing the build argument $name" >&2; exit 1; }; \
    done \
    && for name in VITE_API_BASE_URL VITE_OIDC_AUTHORITY; do \
        eval "value=\${$name}"; \
        case "$value" in */) echo "The build argument $name must not end with /" >&2; exit 1;; esac; \
    done \
    && npm run build

FROM caddy:2-alpine
ARG VITE_API_BASE_URL
ARG VITE_OIDC_AUTHORITY
# The connect-src of the Content-Security-Policy of the Caddyfile: the API and the OpenID Connect provider, with every
# path under them
ENV CSP_CONNECT_SRC="${VITE_API_BASE_URL}/ ${VITE_OIDC_AUTHORITY}/"
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /dashboard/dist /srv
EXPOSE 80
# The admin API of Caddy, inside the container. By the IP, as localhost is ::1 for the wget of the image and the API
# listens on 127.0.0.1.
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:2019/config/ || exit 1
