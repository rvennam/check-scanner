FROM node:6.10.0-alpine

ADD . /app

ENV NODE_ENV production
ENV PORT 3000

EXPOSE 3000

WORKDIR "/app"

RUN npm install
CMD ["npm", "start"]
