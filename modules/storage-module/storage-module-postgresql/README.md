```shell
docker run --name jpom-postgres \
-e POSTGRES_USER=jpom \
-e POSTGRES_PASSWORD=jpom123456 \
-e POSTGRES_DB=jpom \
-p 5432:5432 -d postgres
```