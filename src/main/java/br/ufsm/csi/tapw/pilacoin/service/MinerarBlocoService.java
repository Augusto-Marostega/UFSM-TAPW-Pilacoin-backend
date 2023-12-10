package br.ufsm.csi.tapw.pilacoin.service;

import br.ufsm.csi.tapw.pilacoin.model.Dificuldade;
import br.ufsm.csi.tapw.pilacoin.model.json.BlocoJson;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import br.ufsm.csi.tapw.pilacoin.util.RSAKeyPairGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MinerarBlocoService {
    private static final Logger logger = LoggerFactory.getLogger(MinerarBlocoService.class);
    private final PilacoinDataHandler pilacoinDataHandler;
    private final RSAKeyPairGenerator rsaKeyPairGenerator;
    private final DificuldadeService dificuldadeService;
    private final RabbitMQService rabbitMQService;
    private final AtomicBoolean mineracaoParada = new AtomicBoolean(false);

    @Autowired
    public MinerarBlocoService(
            PilacoinDataHandler pilacoinDataHandler,
            RSAKeyPairGenerator rsaKeyPairGenerator,
            DificuldadeService dificuldadeService,
            RabbitMQService rabbitMQService) {
        this.pilacoinDataHandler = pilacoinDataHandler;
        this.rsaKeyPairGenerator = rsaKeyPairGenerator;
        this.dificuldadeService = dificuldadeService;
        this.rabbitMQService = rabbitMQService;
    }

    public CompletableFuture<Void> minerarBlocoAsync(String strBlocoJson) {
        try {
            return CompletableFuture.runAsync(() -> {
                logger.info("[minerarBlocoAsync] Iniciando mineração de bloco.");
                minerarBloco(strBlocoJson);
            });
        } catch (Exception e) {
            logger.error("[minerarBlocoAsync] Erro ao iniciar mineração assíncrona.", e);
            return CompletableFuture.completedFuture(null);
        }
    }

    public void pararMineracao() {
        mineracaoParada.set(true);
    }

    private void minerarBloco(String strBlocoJson) { //DESCOBRIR BLOCO
        try {
            KeyPair keyPair = rsaKeyPairGenerator.generateOrLoadKeyPair();
            Dificuldade ultimaDificuldade = dificuldadeService.getUltimaDificuldade();

            Random rnd = new SecureRandom();
            PublicKey chaveCriador = keyPair.getPublic();
            String nomeCriador = "iris_augusto";

            if (ultimaDificuldade == null || ultimaDificuldade.getValidadeFinal() == null){
                logger.error("[minerarBloco] ultimaDificuldade é null.");
                return;
            }
            if (!dificuldadeService.estaValida()){
                logger.error("[minerarBloco] ultimaDificuldade não está valida.");
                return;
            }

            BlocoJson blocoJson = pilacoinDataHandler.strParaObjBlocoJson(strBlocoJson);
            blocoJson.setChaveUsuarioMinerador(chaveCriador.getEncoded());
            blocoJson.setNomeUsuarioMinerador(nomeCriador);
            while (!mineracaoParada.get()) {
                String nonce = String.valueOf(new BigInteger(256, rnd).abs());
                blocoJson.setNonce(nonce);
                String blocoJsonString = pilacoinDataHandler.objParaStringJson(blocoJson); //convertendo objeto em JSON
                BigInteger hashBigInt = pilacoinDataHandler.getHashBigInteger(blocoJsonString); //gerando HASH da String JSON

                if (hashBigInt.compareTo(ultimaDificuldade.getDificuldade()) < 0) {
                    // Bloco foi minerado com sucesso
                    logger.info("[minerarBloco] Bloco minerado com sucesso: {}", blocoJsonString);

                    rabbitMQService.enviarMensagemParaFila("bloco-minerado", blocoJsonString);
                    return;
                }
            }
            //logger.warn("[minerarBloco] Saindo do processo de mineração de bloco.");
        } catch (Exception e) {
            logger.error("[minerarBloco] Erro durante a mineração de bloco.", e);
        }
    }

}
