package br.edu.ifs.web1.coletaseletiva.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.ifs.web1.coletaseletiva.model.PontoModel;

public interface PontoRepository extends JpaRepository<PontoModel, Long> {

    @Query("select p from PontoModel p where :ativo is null or p.ativo = :ativo")
    Page<PontoModel> listar(@Param("ativo") Boolean ativo, Pageable pageable);
}
