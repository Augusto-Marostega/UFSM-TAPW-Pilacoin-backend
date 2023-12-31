package br.ufsm.csi.tapw.pilacoin.repository;

import br.ufsm.csi.tapw.pilacoin.model.LogLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface LogLocalRepository extends JpaRepository<LogLocal, BigInteger> {
    List<LogLocal> findFirst1000ByOrderByDataCriacaoDesc();
}
