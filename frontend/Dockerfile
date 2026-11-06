# 멀티스테이지 빌드
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
# 비root 권한 부여 및 설정
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html
COPY --from=build /app/dist /usr/share/nginx/html
# Nginx 설정 파일 복사 필요 시 주석 해제: COPY nginx.conf /etc/nginx/conf.d/default.conf
USER nginx
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s CMD curl -f http://localhost/ || exit 1
CMD ["nginx", "-g", "daemon off;"]