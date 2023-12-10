package br.ufsm.csi.tapw.pilacoin.service;

import br.ufsm.csi.tapw.pilacoin.model.Dificuldade;
import br.ufsm.csi.tapw.pilacoin.model.Pilacoin;
import br.ufsm.csi.tapw.pilacoin.model.json.PilacoinJson;
import br.ufsm.csi.tapw.pilacoin.repository.PilacoinRepository;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import br.ufsm.csi.tapw.pilacoin.util.RSAKeyPairGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MinerarPilacoinService {

    private static final Logger logger = LoggerFactory.getLogger(MinerarPilacoinService.class);

    private final RSAKeyPairGenerator rsaKeyPairGenerator;
    private final DificuldadeService dificuldadeService;
    private final RabbitMQService rabbitMQService;
    private final AtomicBoolean mineracaoParada = new AtomicBoolean(false);
    private final PilacoinDataHandler pilacoinDataHandler;
    private final PilacoinRepository pilacoinRepository;

    @Autowired
    public MinerarPilacoinService(
            PilacoinDataHandler pilacoinDataHandler,
            RSAKeyPairGenerator rsaKeyPairGenerator,
            DificuldadeService dificuldadeService,
            RabbitMQService rabbitMQService,
            PilacoinRepository pilacoinRepository) {
        this.pilacoinDataHandler = pilacoinDataHandler;
        this.rsaKeyPairGenerator = rsaKeyPairGenerator;
        this.dificuldadeService = dificuldadeService;
        this.rabbitMQService = rabbitMQService;
        this.pilacoinRepository = pilacoinRepository;
    }

    @Async
    public CompletableFuture<Void> minerarPilacoinAsync() {
        try {
            return CompletableFuture.runAsync(() -> {
                logger.info("[minerarPilacoinAsync] Iniciando minerarPilaCoin();");
                minerarPilacoin();
            });
        } catch (Exception e) {
            logger.error("[minerarPilacoinAsync] Erro ao iniciar mineração assíncrona.", e);
            return CompletableFuture.completedFuture(null);
        }
    }

    public void pararMineracao() {
        mineracaoParada.set(true);
    }

    private void minerarPilacoin() {
        try {
            while(!mineracaoParada.get()) {
                KeyPair keyPair = rsaKeyPairGenerator.generateOrLoadKeyPair();
                Dificuldade ultimaDificuldade = dificuldadeService.getUltimaDificuldade();
                Random rnd = new SecureRandom();
                PublicKey chaveCriador = keyPair.getPublic();
                String nomeCriador = "iris_augusto";

                if (ultimaDificuldade == null || ultimaDificuldade.getValidadeFinal() == null) {
                    logger.error("[minerarPilacoin] ultimaDificuldade é null.");
                    //pararMineracao();
                }
                if (!dificuldadeService.estaValida()) {
                    logger.error("[minerarPilacoin] ultimaDificuldade não está valida.");
                    //pararMineracao();
                }
                PilacoinJson pilaCoinJson = PilacoinJson.builder()
                        .chaveCriador(chaveCriador.getEncoded())
                        .nomeCriador(nomeCriador)
                        .build();

                int contador = 0;
                while (!mineracaoParada.get()) {
                    contador++;
                    String nonce = String.valueOf(new BigInteger(256, rnd).abs());
                    pilaCoinJson.setDataCriacao(new Date());
                    pilaCoinJson.setNonce(nonce);
                    // Converter PilacoinJson para String JSON
                    String pilacoinJsonString = pilacoinDataHandler.objParaStringJson(pilaCoinJson);
                    BigInteger hashBigInt = pilacoinDataHandler.getHashBigInteger(pilacoinJsonString); //gerando HASH da String JSON
                    if (hashBigInt.compareTo(ultimaDificuldade.getDificuldade()) < 0) {
                        // Pilacoin minerado com sucesso
                        logger.info("[minerarPilacoin] Pilacoin minerado com sucesso nº tentativas: {} PilaCoin: {}", contador, pilacoinJsonString);
                        Pilacoin pilacoin = pilacoinDataHandler.pilacoinJsonParaPilacoin(pilaCoinJson);
                        pilacoinRepository.saveAndFlush(pilacoin);
                        enviarPilaCoinParaFila(pilacoinJsonString);
                        break;  // Saia do loop se o Pilacoin for minerado com sucesso
                    }
                }
            }
            logger.warn("[minerarPilacoin] saindo do processo de mineração do PilaCoin.");

        } catch (Exception e) {
            logger.error("[minerarPilacoin] Erro durante a mineração do Pilacoin.", e);
        }
    }

    private void enviarPilaCoinParaFila(String pilaCoinJsonString) {
        rabbitMQService.enviarMensagemParaFila("pila-minerado", pilaCoinJsonString);
        logger.info("[enviarPilaCoinParaFila] Pilacoin minerado enviado para a fila 'pila-minerado'.");
    }
}
