FROM node:22-bookworm-slim AS build
WORKDIR /src
COPY admin-web/package.json admin-web/package-lock.json ./admin-web/
RUN npm --prefix admin-web ci --no-audit --no-fund
COPY admin-web/index.html ./admin-web/
COPY admin-web/tsconfig.json ./admin-web/
COPY admin-web/src ./admin-web/src
COPY admin-web/scripts ./admin-web/scripts
COPY packages/api-contract/typescript/tsconfig.json ./packages/api-contract/typescript/
COPY packages/api-contract/typescript/src ./packages/api-contract/typescript/src
COPY docs/api/openapi-v1.yaml ./docs/api/openapi-v1.yaml
RUN npm --prefix admin-web run build

FROM node:22-bookworm-slim
WORKDIR /app
RUN mkdir -p /var/lib/fixmate-admin && chown node:node /var/lib/fixmate-admin
COPY --from=build /src/admin-web/dist ./dist
COPY --from=build /src/admin-web/scripts/serve.mjs /src/admin-web/scripts/session.mjs ./scripts/
USER node
CMD ["node","scripts/serve.mjs"]
