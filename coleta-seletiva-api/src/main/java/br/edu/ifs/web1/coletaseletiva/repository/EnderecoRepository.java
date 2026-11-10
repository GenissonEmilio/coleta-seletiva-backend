package br.edu.ifs.web1.coletaseletiva.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifs.web1.coletaseletiva.model.EnderecoModel;

public interface EnderecoRepository extends JpaRepository<EnderecoModel, Long> {

    Page<EnderecoModel> findByMoradorId(Long moradorId, Pageable pageable);
    Optional<EnderecoModel> findByIdAndMoradorId(Long id, Long moradorId);

}
