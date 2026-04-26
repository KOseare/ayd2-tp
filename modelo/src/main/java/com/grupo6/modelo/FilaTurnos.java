package com.grupo6.modelo;

import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FilaTurnos {

  private final Deque<Turno> enEspera = new ArrayDeque<>();
  private final Set<String> dniEnEspera = new HashSet<>();
  private final Map<String, Turno> turnoPorEstacion = new HashMap<>();
  private final List<Turno> turnosLlamados = new ArrayList<>();
  private int nextTurnId = 1;

  public boolean existeClienteEnFila(Cliente cliente) {
    return dniEnEspera.contains(cliente.getDni());
  }

  public boolean estaClienteEnAtencion(Cliente cliente) {
    String dni = cliente.getDni();
    for (Turno t : turnoPorEstacion.values()) {
      if (dni.equals(t.getDni())) {
        return true;
      }
    }
    return false;
  }

  public Turno registrarCliente(Cliente cliente) {
    Turno t = new Turno(nextTurnId++, cliente, new Timestamp(System.currentTimeMillis()));
    enEspera.addLast(t);
    dniEnEspera.add(cliente.getDni());
    return t;
  }

  public int obtenerCantidadTurnos() {
    return enEspera.size();
  }

  public List<Turno> obtenerTurnosLlamados(int n) {
    if (n <= 0) {
      return Collections.emptyList();
    }
    int from = Math.max(0, turnosLlamados.size() - n);
    return new ArrayList<>(turnosLlamados.subList(from, turnosLlamados.size()));
  }

  public NuevoLlamado llamarSiguienteEnEstacion(String estacion) {
    Turno enEstacion = turnoPorEstacion.get(estacion);
    if (enEspera.isEmpty()) {
      if (enEstacion == null) {
        return NuevoLlamado.sinPendientes();
      }
      return NuevoLlamado.sinPendientesMantenerActual(enEstacion.getDni());
    }

    Turno reemplazado = enEstacion;
    if (reemplazado != null) {
      turnoPorEstacion.remove(estacion);
      reemplazado.setEstacion(null);
    }

    Turno siguiente = enEspera.removeFirst();
    dniEnEspera.remove(siguiente.getDni());

    siguiente.setEstado(EstadoTurno.Llamado);
    siguiente.setEstacion(estacion);
    siguiente.setNroLlamados(0);
    turnoPorEstacion.put(estacion, siguiente);
    turnosLlamados.add(siguiente);

    return NuevoLlamado.asignado(reemplazado, siguiente);
  }

  public Renotificacion renotificarEnEstacion(String estacion) {
    Turno t = turnoPorEstacion.get(estacion);
    if (t == null) {
      return Renotificacion.sinActivo();
    }
    t.incrementNroLlamados();
    int intentos = t.getNroLlamados();
    if (intentos >= 3) {
      turnoPorEstacion.remove(estacion);
      t.setEstacion(null);
      return Renotificacion.removidoPorLimite(t.getDni());
    }
    return Renotificacion.notificado(t.getDni(), intentos);
  }

  public Optional<Turno> finalizarEnEstacion(String estacion) {
    Turno t = turnoPorEstacion.remove(estacion);
    if (t == null) {
      return Optional.empty();
    }
    t.setEstado(EstadoTurno.Atendido);
    t.setEstacion(null);
    return Optional.of(t);
  }
}
