# Dockerfile to run the sample under current Node LTS
#
# docker build . -t node-rdkafka
# docker run --rm -it -e VCAP_SERVICES=${VCAP_SERVICES} node-rdkafka
# OR
# docker run --rm -it node-rdkafka <kafka_brokers_sasl> <api_key> /etc/ssl/certs
#
FROM ubuntu:18.04

RUN  apt-get update -qqy \
  && apt-get install -y --no-install-recommends \
     build-essential \
     node-gyp \
     nodejs-dev \
     libssl1.0-dev \
     liblz4-dev \
     libpthread-stubs0-dev \
     libsasl2-dev \
     libsasl2-modules \
     make \
     python \
     nodejs npm ca-certificates \
  && rm -rf /var/cache/apt/* /var/lib/apt/lists/*


ENV TESSDATA_PREFIX /usr/src/app/

RUN apt-get install -y python build-essential
RUN apt update && apt install -y libsm6 libxext6
RUN apt-get -y install tesseract-ocr
RUN tesseract --version

WORKDIR /usr/src/app

COPY *.js *.json *.md /usr/src/app/
COPY mcr2.traineddata *.md /usr/src/app/
COPY images /usr/src/app/images 
RUN chmod a+rw -R /usr/src/app/images

RUN npm install -d
ENV LD_LIBRARY_PATH=/usr/src/app/node_modules/node-rdkafka/build/deps
ENTRYPOINT [ "node", "app.js" ]
CMD [ "" ]
