package br.ufsm.csi.tapw.pilacoin.repository;

import br.ufsm.csi.tapw.pilacoin.model.Pilacoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PilacoinRepository extends JpaRepository<Pilacoin, String> {
    Pilacoin findByNonce(String nonce);
}
