FROM gcr.io/distroless/java17-debian13

WORKDIR /root/.m2/repository
COPY build/image/m2/repository/ /root/.m2/repository/
