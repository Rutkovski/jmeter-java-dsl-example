Запустить тест из идеи
зайти в
src/test/java/ru/rustore/loadtests/googletest/AppTests.java
и кликнуть в  разделе smoke

Запустить тест из под gradle
gradle clean test -PTAGS=google-smoke

Собрать образ
docker build  -t jmeter-java-dsl:v1.0 .

Запустить контейнер:
docker run -v ./src:/tests/src -it -t jmeter-java-dsl:v1.0 sh


Теперь попробовать запустить в оффлайн режиме (добавить команду --offline)
gradle clean test -PTAGS=google-smoke --offline

Теперь попробовать выйти из контейнера и вновь создать его на основе образа из репозитория:
docker run -v ./src:/tests/src -v ./build.gradle:/tests/build.gradle -it -t registry-gitlab.corp.mail.ru/rustore-tools/jmeter-java-dsl:v1.0-gradle sh
И  запустить тест
gradle clean test -PTAGS=google-smoke --offline

!!!!И ЗДЕСЬ МЫ ПОЛУЧАЕМ ОШИБКУ
Execution failed for task ':compileTestJava'.
> Could not resolve all files for configuration ':testCompileClasspath'.
> Could not download commons-configuration2-2.10.1.jar (org.apache.commons:commons-configuration2:2.10.1): No cached version available for offline mode
> Could not download jmeter-java-dsl-jdbc-1.29.1.jar (us.abstracta.jmeter:jmeter-java-dsl-jdbc:1.29.1): No cached version available for offline mode
> Could not download jmeter-java-dsl-1.29.1.jar (us.abstracta.jmeter:jmeter-java-dsl:1.29.1): No cached version available for offline mode
> Could not download ApacheJMeter_http-5.5.jar (org.apache.jmeter:ApacheJMeter_http:5.5): No cached version available for offline mode
> Could not download ApacheJMeter_functions-5.5.jar (org.apache.jmeter:ApacheJMeter_functions:5.5): No cached version available for offline mode
> Could not download jmeter-plugins-random-csv-data-set-0.8.jar (com.blazemeter:jmeter-plugins-random-csv-data-set:0.8): No cached version available for offlin...
> 

