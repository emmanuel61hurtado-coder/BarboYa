# ---- Stage 1: Build ----
FROM node:20-alpine AS builder
WORKDIR /app

# Vite needs env vars at build time (they are baked into the JS bundle)
ARG VITE_API_URL=/api/v1
ARG VITE_USE_MOCKS=false
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_USE_MOCKS=$VITE_USE_MOCKS

COPY package*.json ./
RUN npm ci --ignore-scripts
COPY . .
RUN npm run build

# ---- Stage 2: Serve ----
FROM nginx:stable-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
