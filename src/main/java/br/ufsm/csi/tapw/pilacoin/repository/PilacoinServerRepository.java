package br.ufsm.csi.tapw.pilacoin.repository;

import br.ufsm.csi.tapw.pilacoin.model.PilacoinServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PilacoinServerRepository extends JpaRepository<PilacoinServer, String> {
    PilacoinServer findByNonce(String nonce);
}
