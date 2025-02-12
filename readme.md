Запустить тест из идеи
зайти в
src/test/java/ru/rustore/loadtests/googletest/AppTests.java
и кликнуть в  разделе smoke

Запустить тест из под gradle
gradle clean test -PTAGS=google-smoke

Собрать образ
docker build  -t jmeter-java-dsl:v1.0 .

Запустить контейнер:
docker run -v ./src:/tests/src -v ./build.gradle:/tests/build.gradle -it -t jmeter-java-dsl:v1.0 sh

Внутри контейнера проверить, что затянулись закэшированные зависимости
ls -lAh /home/gradle/.gradle/caches/modules-2/files-2.1

Проверить что тест запускается из-под контейнера выполнив внутри его
gradle clean test -PTAGS=google-smoke

Если в логе увидел что-то в духе ниже, то тест выполнился успешно:
Gradle Test Run :test > Gradle Test Executor 1 > AppTests > smokeTest() STANDARD_OUT
+     69 in 00:00:23 =    3.0/s Avg:   987 Min:   797 Max:  1968 Err:     0 (0.00%) Active: 3 Started: 3 Finished: 0

Теперь попробовать запустить в оффлайн режиме (добавить команду --offline или выключить интернет)
gradle clean test -PTAGS=google-smoke --offline

Если ты уже запускал в первый раз, то так должно сработать!

Теперь попробовать выйти из контейнера и вновь создать его:
docker run -v ./src:/tests/src -v ./build.gradle:/tests/build.gradle -it -t jmeter-java-dsl:v1.0 sh
И сразу запустить с параметром --offline
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

Т.е. зависимости вроде как закэшировались на этапе сборки, и есть внутри контейнера, но градл их не видит, если запускаешь без параметра --offline то он их выкачивает и дальше работает нормально
Задача: сделать так, чтобы собранный образ мог запускаться offline, т.к. на прод среде у него не будет доступа к интернету