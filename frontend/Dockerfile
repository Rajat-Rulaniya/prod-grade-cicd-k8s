## Stage: 1 building the static files ------
FROM node:alpine AS builder

WORKDIR /app

COPY ./package*.json ./

RUN npm ci

COPY ./src ./public ./

RUN npm run build

## Stage: 2 Serving the static files via nginx

FROM nginx:stable-alpine AS production

RUN rm -rf /usr/share/nginx/html/*

COPY --from=builder /app/build/ /usr/share/nginx/html/

COPY ./src/config.template.js /usr/share/nginx/html/config.template.js

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
