package br.ufsm.csi.tapw.pilacoin.service;

import br.ufsm.csi.tapw.pilacoin.model.Dificuldade;
import br.ufsm.csi.tapw.pilacoin.model.LogLocal;
import br.ufsm.csi.tapw.pilacoin.model.json.DificuldadeJson;
import br.ufsm.csi.tapw.pilacoin.repository.LogLocalRepository;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Date;

@Service
public class DificuldadeService {

    private static final Logger logger = LoggerFactory.getLogger(DificuldadeService.class);
    private final LogLocalRepository logLocalRepository;
    private final PilacoinDataHandler pilacoinDataHandler;
    private Dificuldade ultimaDificuldade;

    @Autowired
    public DificuldadeService(LogLocalRepository logLocalRepository, PilacoinDataHandler pilacoinDataHandler) {
        this.logLocalRepository = logLocalRepository;
        this.pilacoinDataHandler = pilacoinDataHandler;
    }

    public void salvarDificuldade(String strDificuldadeJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            DificuldadeJson dificuldadeJson = objectMapper.readValue(strDificuldadeJson, DificuldadeJson.class);

            Dificuldade dificuldadeAtual = new Dificuldade(new BigInteger(dificuldadeJson.getDificuldade(), 16), dificuldadeJson.getInicio(), dificuldadeJson.getValidadeFinal());

            if (this.ultimaDificuldade == null || !dificuldadeAtual.equals(this.ultimaDificuldade)) {
                // Atualize a dificuldade atual
                this.ultimaDificuldade = dificuldadeAtual;
                logger.info("[salvarDificuldade] A dificuldade foi atualizada JSON: {}", strDificuldadeJson);
                LogLocal loglocal = LogLocal.builder()
                        .tipo("dificuldade")
                        .dataCriacao(new Date())
                        .status("info")
                        .conteudo(" A dificuldade foi atualizada JSON: " + pilacoinDataHandler.reduzirString(strDificuldadeJson))
                        .build();
                logLocalRepository.saveAndFlush(loglocal);
                // Realize ações necessárias com a nova dificuldade
            } else {
                //logger.info("A dificuldade não foi alterada.");
            }
        } catch (JsonProcessingException e) {
            logger.error("[salvarDificuldade] Erro ao processar JSON da dificuldade", e);
            // Tratar a exceção de acordo com os requisitos do seu aplicativo
        }
    }

    public Dificuldade getUltimaDificuldade() {
        return ultimaDificuldade;
    }

    public boolean estaValida(){
        Date data = new Date();
        return true;
        //return data.after(this.ultimaDificuldade.getInicio()) && data.before(this.ultimaDificuldade.getValidadeFinal());
    }
}