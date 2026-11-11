package br.edu.ifs.web1.coletaseletiva.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifs.web1.coletaseletiva.model.OperadorModel;

public interface OperadorRepository extends JpaRepository<OperadorModel, Long> {

    boolean existsByEmail(String email);

    Optional<OperadorModel> findByEmail(String email);
}
