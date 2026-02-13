# DukeOps

[![All Tests](https://github.com/ijug-ev/dukeops/actions/workflows/all-tests.yml/badge.svg)](https://github.com/ijug-ev/dukeops/actions/workflows/all-tests.yml)

**iJUG Self-Service Portal**

## Table of Contents

- [About](#about)
- [Versioning](#versioning)
- [Contributing](#contributing)
- [Communication](#communication)
    - [Matrix Chat](#matrix-chat)
    - [GitHub Discussions](#github-discussions)
- [Configuration](#configuration)
    - [Server Configuration](#server-configuration)
    - [Mail Configuration](#mail-configuration)
    - [Database Configuration](#database-configuration)
- [Copyright and License](#copyright-and-license)

## About

*DukeOps* is a self-service portal for iJUG members.

## Versioning

*DukeOps* follows [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html). Version numbers are structured as `MAJOR.MINOR.PATCH`:

- **MAJOR** versions introduce incompatible API or data changes,
- **MINOR** versions add functionality in a backwards-compatible manner,
- **PATCH** versions include backwards-compatible bug fixes.

This versioning scheme applies to the public API, database schema, and plugin interfaces. We aim to keep upgrades predictable and manageable. Breaking changes, new features, and fixes are documented in the [CHANGELOG.md](CHANGELOG.md) and in the [release notes](https://github.com/ijug-ev/dukeops/releases).

## Contributing

You can find a lot of information on how you can contribute to *DukeOps* in the separate file [CONTRIBUTING.md](CONTRIBUTING.md). A curated list of contributors is available in the file [CONTRIBUTORS.md](CONTRIBUTORS.md).

## Communication

### Matrix Chat

There is a channel at Matrix for quick and easy communication. This is publicly accessible for everyone. For developers as well as users. The communication in this chat is to be regarded as short-lived and has no documentary character.

You can find our Matrix channel here: [@dukeops:ijug.eu](https://matrix.to/#/%23dukeops:ijug.eu)

### GitHub Discussions

We use the corresponding GitHub function for discussions. The discussions held here are long-lived and divided into categories for the sake of clarity. One important category, for example, is that for questions and answers.

Discussions on GitHub: https://github.com/ijug-ev/dukeops/discussions  
Questions and Answers: https://github.com/ijug-ev/dukeops/discussions/categories/q-a

## Configuration

The file `application.properties` contains only some default values. To override the default values and to specify other configuration options, just set them as environment variables. The following sections describe all available configuration options. You only need to specify these options if your configuration settings differ from the defaults.

### Server Configuration

The server runs on port 8080 by default. If you don't like it, change it using an environment variable:

```
DUKEOPS_PORT=8080
```

### Instance Configuration

#### Admin

When starting *DukeOps*, the system can automatically create an instance admin. To enable this, set the following environment variable to the email address of the admin:

```
DUKEOPS_INSTANCE_ADMIN=admin@example.com
```

If no user with the admin role is found in the database, a new instance admin will be created with the email address given.

> [!WARNING]
> This mechanism runs once on each start! It will **not** overwrite or update existing users with the same email address.

### Mail Configuration

*DukeOps* supports sending email notifications. Configuration is done via environment variables using the `DUKEOPS_MAIL_*` naming scheme.

#### Available Environment Variables

| Variable                        | Default             | Description                                                           |
|---------------------------------|---------------------|-----------------------------------------------------------------------|
| `DUKEOPS_MAIL_FROM`              | `noreply@localhost` | Sender address shown in outgoing emails (e.g. noreply@example.com).   |
| `DUKEOPS_MAIL_REPLY_TO`          | *(empty)*           | Optional reply-to address (e.g. `support@example.com`).               |
| `DUKEOPS_MAIL_HOST`              | `localhost`         | Mail server address. Use a local MTA or external SMTP provider.       |
| `DUKEOPS_MAIL_PORT`              | `25`                | Port for the SMTP server (e.g., `587` for STARTTLS or `465` for SSL). |
| `DUKEOPS_MAIL_PROTOCOL`          | `smtp`              | Protocol used for sending email. Usually `smtp`.                      |
| `DUKEOPS_MAIL_USERNAME`          | *(empty)*           | Username for SMTP authentication, if required.                        |
| `DUKEOPS_MAIL_PASSWORD`          | *(empty)*           | Password for SMTP authentication, if required.                        |
| `DUKEOPS_MAIL_SMTP_AUTH`         | `false`             | Whether SMTP authentication is enabled.                               |
| `DUKEOPS_MAIL_STARTTLS_ENABLE`   | `false`             | Enable STARTTLS encryption (recommended for port 587).                |
| `DUKEOPS_MAIL_STARTTLS_REQUIRED` | `false`             | Require STARTTLS (connection will fail if not supported).             |
| `DUKEOPS_MAIL_SSL_ENABLE`        | `false`             | Enable SSL encryption (typically for port 465).                       |
| `DUKEOPS_MAIL_CONNECTION_TIMEOUT` | `5000`              | Timeout in milliseconds for establishing the SMTP connection. |
| `DUKEOPS_MAIL_TIMEOUT`            | `5000`              | Timeout in milliseconds for waiting on SMTP server responses. |
| `DUKEOPS_MAIL_WRITE_TIMEOUT`      | `5000`              | Timeout in milliseconds for writing data to the SMTP server. |
| `DUKEOPS_MAIL_ENCODING`          | `UTF-8`             | Default encoding for email subject and content.                       |

#### Example Configuration

In a `.env` file, CI system, or Docker environment:

```bash
DUKEOPS_INSTANCE_ADMIN=admin@example.com
DUKEOPS_MAIL_FROM=noreply@example.com
DUKEOPS_MAIL_REPLY_TO=support@example.com
DUKEOPS_MAIL_HOST=smtp.example.com
DUKEOPS_MAIL_PORT=587
DUKEOPS_MAIL_USERNAME=myuser
DUKEOPS_MAIL_PASSWORD=secret
DUKEOPS_MAIL_SMTP_AUTH=true
DUKEOPS_MAIL_STARTTLS_ENABLE=true
```

> [!TIP]
> If you are using a local mail relay (e.g., [Nullmailer](https://untroubled.org/nullmailer/) or [Postfix](https://www.postfix.org/)), you can often omit authentication and encryption settings.

### Database Configuration

*DukeOps* needs a database to store the business data. By default, *DukeOps* comes with [MariaDB](https://mariadb.org/) drivers. MariaDB is recommended because we are using it during development, and it is highly tested with *DukeOps*. All free and open source JDBC compatible databases are supported, but you need to configure the JDBC driver dependencies accordingly. Please make sure that your database is using a Unicode character set to avoid problems storing data containing Unicode characters. The database user to access the *DukeOps* database executes automatic schema migrations and needs `ALL PRIVILEGES`.

Please configure the database connection using the following environment variables:

```
DUKEOPS_DB_URL=jdbc:mariadb://localhost:3306/dukeops?serverTimezone\=Europe/Zurich&allowMultiQueries=true
DUKEOPS_DB_USER=johndoe
DUKEOPS_DB_PASS=verysecret
```

The database schema will be migrated automatically by *DukeOps*.

#### Important MySQL and MariaDB configuration

MySQL and MariaDB have a possible silent truncation problem with the `GROUP_CONCAT` command. To avoid this it is necessary, to configure these two databases to allow multi queries. Just add `allowMultiQueries=true` to the JDBC database URL like in this example (you may need to scroll the example code to the right):

```
DUKEOPS_DB_URL=jdbc:mariadb://localhost:3306/dukeops?serverTimezone\=Europe/Zurich&allowMultiQueries=true
```

## Copyright and License

[AGPL License](https://www.gnu.org/licenses/agpl-3.0.de.html)

*Copyright (C) Marcus Fihlon and the individual contributors to **DukeOps**.*

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
