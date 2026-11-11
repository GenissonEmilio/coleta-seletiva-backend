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

    /**
     * Consulta pública: só pontos ativos. Filtro vazio ("") é ignorado.
     * O material é filtrado com EXISTS, e não com JOIN, para um ponto nunca aparecer repetido na página.
     */
    @Query("""
            select p from PontoModel p
            where p.ativo = true
            and lower(p.nome) like concat('%', :nome, '%')
            and (:municipio = '' or lower(p.municipio) = :municipio)
            and (:bairro = '' or lower(p.bairro) = :bairro)
            and (:materialId is null or exists (
                select 1 from PontoMaterialModel pm
                where pm.id.pontoId = p.id and pm.id.materialId = :materialId and pm.material.ativo = true))
            """)
    Page<PontoModel> buscarPublico(@Param("nome") String nome, @Param("municipio") String municipio,
            @Param("bairro") String bairro, @Param("materialId") Long materialId, Pageable pageable);
}
