# Sistema de Gestión de Filas

## Compilar

```bash
mvn clean install
```

## Ejecución

### Monitor de Sala

```bash
mvn -pl monitor_de_sala exec:java -Dexec.mainClass="com.grupo6.monitor_de_sala.Main"
```

### Interfaz de Operador

```bash
mvn -pl interfaz_de_operador exec:java -Dexec.mainClass="com.grupo6.interfaz_de_operador.Main"
```

### Terminal de Registro

```bash
mvn -pl terminal_de_registro exec:java -Dexec.mainClass="com.grupo6.terminal_de_registro.Main"
```
