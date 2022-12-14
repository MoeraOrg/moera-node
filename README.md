# Moera Node

Live network: https://web.moera.org

Read more about Moera at https://moera.org

Learn more about Moera nodes: http://moera.org/overview/node.html

Bugs and feature requests: https://github.com/MoeraOrg/moera-issues/issues

How to setup a complete Moera Development Environment:
http://moera.org/development/setup/index.html

Installation instructions:

1. As prerequisites you need to have Java 11+ and PostgreSQL 9.6+
   installed. In all major Linux distributions you can install them from
   the main package repository.
2. Create a PostgreSQL user `<username>` with password `<password>` and
   an empty database `<dbname>` owned by this user (see detailed
   instructions here:
   http://moera.org/development/setup/create-db.html).
3. Create a directory `<media>`, where the server will keep media files.
4. Go to the source directory.
5. Create `src/main/resources/application-dev.yml` with the following
   content:
   
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql:<dbname>?characterEncoding=UTF-8
       username: <username>
       password: <password>
     flyway:
       user: <username>
       password: <password>
   node:
     root-secret: <secret>
     media:
       path: <media>
   ```

   * `<secret>` must be a long random string of letters and digits
     without spaces.

6. By default, the server runs on port 8081. If you want it to run on a
   different port, add these lines to the file above:
    
   ```yaml
   server:
     port: <port number>
   ```
7. Execute `./run` script.
8. If you use your own [naming server][1], make sure its location is set
   correctly in node settings.

[1]: https://github.com/MoeraOrg/moera-naming
