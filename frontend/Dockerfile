## Stage: 1 building the static files ------
FROM node:alpine AS builder

WORKDIR /app

COPY ./package*.json ./

RUN npm ci

COPY ./ ./

RUN npm run build

## Stage: 2 Serving the static files via nginx

FROM nginx:stable-alpine AS production

RUN rm -rf /usr/share/nginx/html/*

COPY --from=builder /app/build/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
