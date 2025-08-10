## Stage: 1 building the static files ------
FROM node:alpine3.22 as builder

WORKDIR /app

COPY ./package*.json ./

RUN npm ci

COPY ./ ./

RUN npm run build

## Stage: 2 Serving the static files via nginx

FROM nginx:stable-alpine3.21-perl as Production

RUN rm -rf /usr/share/nginx/html/*

COPY --from=builder /app/build/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
