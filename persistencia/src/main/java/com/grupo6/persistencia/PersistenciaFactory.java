package com.grupo6.persistencia;

import com.grupo6.persistencia.entidad.HistEntidad;
import com.grupo6.persistencia.entidad.MapEntidad;
import com.grupo6.persistencia.entidad.QueueEntidad;
import com.grupo6.persistencia.entidad.StationsEntidad;

public abstract class PersistenciaFactory {

  public abstract StationsEntidad createStationsEntidad();

  public abstract QueueEntidad createQueueEntidad();

  public abstract MapEntidad createMapEntidad();

  public abstract HistEntidad createHistEntidad();
}
