package br.edu.ifs.web1.coletaseletiva.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifs.web1.coletaseletiva.model.MoradorModel;

public interface MoradorRepository extends JpaRepository<MoradorModel, Long> {

    boolean existsByEmail(String email);
    Optional<MoradorModel> findByEmail(String email);

}
