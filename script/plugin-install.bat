mvn clean install -Pinstall-plugin-profile

mvn clean install -DskipTests=true deploy -Pinstall-plugin-profile



mvn clean install -DskipTests=true deploy -Prelease-plugin-profile