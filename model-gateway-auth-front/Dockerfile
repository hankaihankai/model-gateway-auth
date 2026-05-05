# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS builder

WORKDIR /build

ENV HUSKY=0

RUN corepack enable && corepack prepare pnpm@10.20.0 --activate

COPY package.json pnpm-lock.yaml .npmrc ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM nginx:1.27-alpine

WORKDIR /usr/share/nginx/html

COPY --from=builder /build/dist ./
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80
