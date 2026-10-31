FROM node:24-alpine

LABEL org.opencontainers.image.title="Nasfonía" \
      org.opencontainers.image.description="Servidor de música en streaming para tu NAS" \
      org.opencontainers.image.source="https://github.com/acarmonafigueroa/nasfonia" \
      org.opencontainers.image.licenses="PolyForm-Noncommercial-1.0.0"

WORKDIR /app
ENV NODE_ENV=production \
    PORT=8090 \
    MUSIC_DIR=/music \
    DATA_DIR=/data

COPY package.json package-lock.json* ./
RUN npm ci --omit=dev --no-audit --no-fund || npm install --omit=dev --no-audit --no-fund

COPY src ./src
COPY public ./public

RUN mkdir -p /data /music && chown -R node:node /data
VOLUME ["/data"]
EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s \
  CMD wget -qO- http://127.0.0.1:8090/api/ping || exit 1

CMD ["node", "--disable-warning=ExperimentalWarning", "src/index.js"]
