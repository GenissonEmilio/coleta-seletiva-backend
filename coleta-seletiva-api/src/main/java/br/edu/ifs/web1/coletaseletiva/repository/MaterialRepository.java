package br.edu.ifs.web1.coletaseletiva.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.ifs.web1.coletaseletiva.model.MaterialModel;

public interface MaterialRepository extends JpaRepository<MaterialModel, Long> {

    @Query("""
            select count(m) > 0 from MaterialModel m
            where lower(trim(m.nome)) = lower(:nome)
            and (:id is null or m.id <> :id)
            """)
    boolean existeNome(@Param("nome") String nome, @Param("id") Long idIgnorado);

    @Query("select m from MaterialModel m where :ativo is null or m.ativo = :ativo")
    Page<MaterialModel> listar(@Param("ativo") Boolean ativo, Pageable pageable);
}
