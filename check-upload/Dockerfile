FROM ubuntu

ARG DEBIAN_FRONTEND=noninteractive

RUN  apt-get update -qqy \
  && apt-get install -y --no-install-recommends \
     build-essential \
     node-gyp \
     nodejs \
     libssl-dev \
     liblz4-dev \
     libpthread-stubs0-dev \
     libsasl2-dev \
     libsasl2-modules \
     make \
     python \
     nodejs npm ca-certificates \
  && rm -rf /var/cache/apt/* /var/lib/apt/lists/*

ADD . /app
WORKDIR /app

ENV NODE_ENV production
ENV PORT 8080
EXPOSE 8080

RUN npm install -d
ENV LD_LIBRARY_PATH=/app/node_modules/node-rdkafka/build/deps
CMD ["npm", "start"]
