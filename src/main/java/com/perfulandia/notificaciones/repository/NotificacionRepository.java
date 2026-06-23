package com.perfulandia.notificaciones.repository;

import com.perfulandia.notificaciones.model.entity.Notificacion;
import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Repositorio JPA para la entidad Notificacion.
 * Proporciona búsquedas paginadas por estado y tipo.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByEstado(EstadoNotificacion estado, Pageable pageable);

    Page<Notificacion> findByTipo(TipoNotificacion tipo, Pageable pageable);

    Page<Notificacion> findByTipoAndEstado(TipoNotificacion tipo, EstadoNotificacion estado, Pageable pageable);
}
