# RiskRegisterHQ

## Pre-requisites

- Install Java 21
- Install MySQL Community Edition
- Create MySQL database using `CREATE DATABASE riskregister CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`


## Building and deployment

Define following environment variables

```
riskregisterhq_email_from=riskregisterhq@company.com
riskregisterhq_email_transactional_id=1
riskregisterhq_java_db_password=
riskregisterhq_java_db_url=jdbc:mysql://127.0.0.1:3306/riskregister?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&connectTimeout=10000&socketTimeout=10000
riskregisterhq_java_db_username=
riskregisterhq_java_email_password=
riskregisterhq_java_email_username=
riskregisterhq_java_hibernate_ddl_auto=update
riskregisterhq_java_mail_host=smtp.yourcompany.com
riskregisterhq_java_server_port=8080
```

Use the following command to build the application

```bash
mvn clean install
```

Run the application using following command:

```bash
mvn spring-boot:run
```


## Development
Use the following command to run the application in development with local sqlite database

```bash
mvn spring-boot:run --define spring-boot.run.arguments="--spring.profiles.active=dev"
```


## Run with demo data

```bash
mvn spring-boot:run -Dspring.profiles.active=demo
mvn spring-boot:run --define spring-boot.run.arguments="--spring.profiles.active=demo"
```

How a risk is defined?

"[cause] may lead to [event], resulting in [consequence]":

That's scoreable, ownable, and you can write a treatment plan against it.

A problem is defined by its relationship to incidents; an issue is defined by its relationship to controls and obligations. 
