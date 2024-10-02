# Scala REST API starter kit

Starter kit with basic REST API operations:
* HTTP routing, middlewares and errors handling; 
* Authentication, permissions and user session;
* Operations with files;
* Database queries and migrations;
* Codebase covered by tests.

#### Tools:
* Main framework: [ZIO](https://zio.dev/overview/getting-started);
* Libraries from ZIO ecosystem: https://zio.dev/ecosystem/officials/;
* Database migrations: [FlywayDB](https://documentation.red-gate.com/flyway/flyway-cli-and-api/concepts/migrations);
* PostgreSQL as database;
* Build tool: [SBT](https://www.scala-sbt.org/)


#### Getting Started

1. Start PostgreSQL database. Check required credentials in `application.conf`;
2. Run `MigrateDatabase` command;
3. Start server using sbt or another build tool.