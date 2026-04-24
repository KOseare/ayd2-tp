# Sistema de Gestión de Filas

## Setup

### Variables de entorno

Las variables de entorno no quedan en el .jar. El sistema en el que se ejecute el .jar debe contener dichas variables de entorno. Es decir, debe haber un .env en el mismo directorio desde donde se ejecute la aplicación.

Crear archivo .env en la raíz del proyecto siguiendo el ejemplo de .env.example.

## Compilar

```bash
mvn clean package
```

## Ejecución

### Monitor de Sala

```bash
mvn -pl monitor_de_sala exec:java -Dexec.mainClass="com.grupo6.monitor_de_sala.Main"
```

```bash
java -jar monitor_de_sala/target/monitor_de_sala-1.0.jar
```

### Interfaz de Operador

```bash
mvn -pl interfaz_de_operador exec:java -Dexec.mainClass="com.grupo6.interfaz_de_operador.Main"
```

```bash
java -jar interfaz_de_operador/target/interfaz_de_operador-1.0.jar
```

### Terminal de Registro

```bash
mvn -pl terminal_de_registro exec:java -Dexec.mainClass="com.grupo6.terminal_de_registro.Main"
```

```bash
java -jar terminal_de_registro/target/terminal_de_registro-1.0.jar
```

### Servidor

```bash
mvn -pl servidor exec:java -Dexec.mainClass="com.grupo6.servidor.Main"
```

```bash
java -jar servidor/target/servidor-1.0.jar
```
