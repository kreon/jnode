**jNode** is a complex portable application, providing all necessary functions (mailer, tosser, tracker) for a FTN system.

It's written in Java (version 1.7 recommended) and distributed under the terms of Apache License 2.0.

Features of the current version:
- sending and receiving bundles using binkp/1.1-compatible protocol
- keeping all the data (links, messages, subscriptions etc.) in the SQL database
- on-the-fly bundle creation
- multithreading
- netmail routing and tracking
- robots
- fileareas support
- simple AreaFix (`+echo`, `-echo`, `%rescan`, `%list`, `%query`, `%help`)
- simple FileFix (`+fecho`, `-fecho`, `%list`, `%query`, `%help`)

jNode uses the [ORMLite](http://ormlite.com) library for SQL database access, thus supporting DB2, Derby, H2, hSQL, mySQL, Netezza, Oracle, PostgreSQL, SQLite, MS SQL Server.

