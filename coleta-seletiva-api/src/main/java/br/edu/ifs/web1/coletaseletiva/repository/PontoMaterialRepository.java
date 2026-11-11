package br.edu.ifs.web1.coletaseletiva.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifs.web1.coletaseletiva.model.PontoMaterialId;
import br.edu.ifs.web1.coletaseletiva.model.PontoMaterialModel;

public interface PontoMaterialRepository extends JpaRepository<PontoMaterialModel, PontoMaterialId> {

    Page<PontoMaterialModel> findByPontoId(Long pontoId, Pageable pageable);
}
