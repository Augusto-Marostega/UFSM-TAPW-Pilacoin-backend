package br.ufsm.csi.tapw.pilacoin.service;

import br.ufsm.csi.tapw.pilacoin.model.Dificuldade;
import br.ufsm.csi.tapw.pilacoin.model.LogLocal;
import br.ufsm.csi.tapw.pilacoin.model.json.PilacoinJson;
import br.ufsm.csi.tapw.pilacoin.model.json.PilacoinValidadoJson;
import br.ufsm.csi.tapw.pilacoin.repository.LogLocalRepository;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import br.ufsm.csi.tapw.pilacoin.util.RSAKeyPairGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Date;

@Service
public class ValidarPilacoinService {
    private static final Logger logger = LoggerFactory.getLogger(ValidarPilacoinService.class);
    private final RSAKeyPairGenerator rsaKeyPairGenerator;
    private final RabbitMQService rabbitMQService;
    private final PilacoinDataHandler pilacoinDataHandler;
    private final DificuldadeService dificuldadeService;
    private final LogLocalRepository logLocalRepository;

    @Autowired
    public ValidarPilacoinService(RSAKeyPairGenerator rsaKeyPairGenerator, PilacoinDataHandler pilacoinDataHandler, RabbitMQService rabbitMQService, DificuldadeService dificuldadeService, LogLocalRepository logLocalRepository) {
        this.rsaKeyPairGenerator = rsaKeyPairGenerator;
        this.pilacoinDataHandler = pilacoinDataHandler;
        this.rabbitMQService = rabbitMQService;
        this.dificuldadeService = dificuldadeService;
        this.logLocalRepository = logLocalRepository;
    }

    public boolean validarPilacoin(String strPilacoinJson) {
        try {
            logger.info("[processarPilacoin] Iniciando validação do Pilacoin: {}", strPilacoinJson);
            if(strPilacoinJson == null || strPilacoinJson.isEmpty()){
                logger.error("Pilacoin esta null");
                return false;
            }
            PilacoinJson pilacoinJson = pilacoinDataHandler.strParaObjPilacoinJson(strPilacoinJson);

            if (pilacoinJson.getNonce() == null || pilacoinJson.getNomeCriador() == null || pilacoinJson.getChaveCriador() == null || pilacoinJson.getDataCriacao() == null) {
                logger.error("[validarPilacoin] Pilacoin com atributos obrigatórios null. Ignorando validação. Retornando false para reenviar para a fila 'pila-minerado'.");
                return false;
            } else if ("iris_augusto".equals(pilacoinJson.getNomeCriador())) {
                // Se o criador é "Augusto", reenvia para a fila "pila-minerado"
                logger.info("[processarPilacoin] Pilacoin criado por iris_augusto. Ignorando validação. Retornando false para reenviar para a fila 'pila-minerado'.");
                return false;
            } else {
                BigInteger hashBigInt = pilacoinDataHandler.getHashBigInteger(strPilacoinJson); // HASH BigInteger gerando da string original
                Dificuldade ultimaDificuldade = dificuldadeService.getUltimaDificuldade();
                if (ultimaDificuldade == null || ultimaDificuldade.getValidadeFinal() == null){
                    logger.error("[processarPilacoin] Não pode validar Pilacoin -- ultimaDificuldade é null ou sem validade final..");
                    return false;
                }
                if (!dificuldadeService.estaValida()){
                    logger.error("[processarPilacoin] Não pode validar Pilacoin -- ultimaDificuldade não está valida.");
                    return false;
                }
                if (hashBigInt.compareTo(ultimaDificuldade.getDificuldade()) < 0) {
                    logger.info("[processarPilacoin] Pilacoin validado, gerando assinatura.");
                    byte[] assinaturaPilacoin = pilacoinDataHandler.gerarAssinatura(pilacoinJson, rsaKeyPairGenerator.generateOrLoadKeyPair().getPrivate()); //assinatura do obj pilacoinJson
                    PilacoinValidadoJson pilacoinValidadoJson = new PilacoinValidadoJson("iris_augusto", rsaKeyPairGenerator.generateOrLoadKeyPair().getPublic().getEncoded(), assinaturaPilacoin, pilacoinJson);
                    //logger.info("[processarPilacoin] Pilacoin assinado JSON: {}", pilacoinDataHandler.pilacoinValidadoJsonParaStrJson(pilacoinValidadoJson));
                    rabbitMQService.enviarMensagemParaFila("pila-validado", pilacoinDataHandler.objParaStringJson(pilacoinValidadoJson));

                    LogLocal loglocal = LogLocal.builder()
                            .tipo("validar_pilacoin")
                            .dataCriacao(new Date())
                            .status("info")
                            .conteudo("Pilacoin Validado com sucesso." + pilacoinDataHandler.reduzirString(" Nome Criador: " + pilacoinJson.getNomeCriador() + " Nonce: " + pilacoinJson.getNonce()))
                            .build();
                    logLocalRepository.saveAndFlush(loglocal);
                    return true;
                }
            }
            //log hash não é menor que a dificuldade atual
            logger.warn("[processarPilacoin] HASH não é menor que dificuldade...");
            LogLocal loglocal = LogLocal.builder()
                    .tipo("validar_pilacoin")
                    .dataCriacao(new Date())
                    .status("error")
                    .conteudo("Pilacoin não validado. HASH não é menor que dificuldade.")
                    .build();
            logLocalRepository.saveAndFlush(loglocal);
            return false;
        } catch (Exception e) {
            logger.error("[processarPilacoin] Erro ao processar Pilacoin: {}", e.getMessage());
            // Em caso de erro, trata de alguma forma (log, lançar exceção, etc.)
            // Pode ser necessário ajustar conforme sua necessidade
            return false;  // Pilacoin não validado devido a erro
        }
    }
}
