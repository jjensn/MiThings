FROM golang:1.6-alpine

# Install some pkackages
RUN apk update && apk add make git

# Fetch godep, testing framework
RUN go get github.com/tools/godep && \
    go get github.com/onsi/gomega && \
    go get github.com/onsi/ginkgo

RUN mkdir -p /go/src/mithings/backend/bin

WORKDIR /go/src/mithings/backend

ENV CGO_ENABLED=0
ENV GOOS=linux

#CMD ["/bin/true"]