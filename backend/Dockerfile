FROM scratch

ADD ./mithings-backend /mithings-backend

EXPOSE 38899

CMD ["/mithings-backend", "--redis-port", "6379", "--log-level", "debug"]