# English Words

This is a pure java application which helps user to remember and repeat translations for english words. To run "EnglishWords" compile src folder and include lib folder into your classpath.

The application uses Model-View-Controller design pattern. Interface is implemented on Java Swing. Controller class is responsible for every interaction in this application.

Model has 2 implementations: file-based and mysql-based. For mysql to work with you need to specify "user.name" and "user.password" in the {"src/resources/mysql/db.properties"} file.

The application is cross-platform and should work on every desktop operating system, e.g. Windows, Mac OS or Ubuntu. The only requirement for application to run is Java 8.

The application downloads pronunciation and definition for every english word.
