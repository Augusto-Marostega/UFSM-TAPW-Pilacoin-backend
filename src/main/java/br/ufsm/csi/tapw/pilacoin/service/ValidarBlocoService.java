package br.ufsm.csi.tapw.pilacoin.service;

import br.ufsm.csi.tapw.pilacoin.model.Dificuldade;
import br.ufsm.csi.tapw.pilacoin.model.json.BlocoJson;
import br.ufsm.csi.tapw.pilacoin.model.json.BlocoValidadoJson;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import br.ufsm.csi.tapw.pilacoin.util.RSAKeyPairGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ValidarBlocoService {

    private static final Logger logger = LoggerFactory.getLogger(ValidarBlocoService.class);

    private final RSAKeyPairGenerator rsaKeyPairGenerator;
    private final DificuldadeService dificuldadeService;
    private final RabbitMQService rabbitMQService;
    private final AtomicBoolean miningStopped = new AtomicBoolean(false);
    private final PilacoinDataHandler pilacoinDataHandler;

    @Autowired
    public ValidarBlocoService(
            PilacoinDataHandler pilacoinDataHandler,
            RSAKeyPairGenerator rsaKeyPairGenerator,
            DificuldadeService dificuldadeService,
            RabbitMQService rabbitMQService) {
        this.pilacoinDataHandler = pilacoinDataHandler;
        this.rsaKeyPairGenerator = rsaKeyPairGenerator;
        this.dificuldadeService = dificuldadeService;
        this.rabbitMQService = rabbitMQService;
    }

    public CompletableFuture<Void> validarBlocoAsync(String strBlocoJson) {
        try {
            return CompletableFuture.runAsync(() -> {
                //logger.info("[validarBlocoAsync] Iniciando Validação de bloco.");
                if(!validarBloco(strBlocoJson)){
                    //erro ao validar, reenviar "strBlocoJson" para a fila bloco-validado
                    logger.error("[validarBlocoAsync] Bloco NÃO Validado, reenviando para a fila 'bloco-minerado'.");
                    rabbitMQService.enviarMensagemParaFila("bloco-minerado", strBlocoJson);
                } else{
                    logger.info("[validarBlocoAsync] Bloco Validado, reenviando para a fila 'bloco-minerado' para outros usuários validarem.");
                    rabbitMQService.enviarMensagemParaFila("bloco-minerado", strBlocoJson);
                }
            });
        } catch (Exception e) {
            logger.error("[validarBlocoAsync] Erro ao iniciar validação de bloco assíncrona.", e);
            return CompletableFuture.completedFuture(null);
        }
    }

    public void stopMining() {
        miningStopped.set(true);
    }

    private boolean validarBloco(String strBlocoJson) {
        try {
            logger.info("[validarBloco] Iniciando validação do Bloco: {}", strBlocoJson);
            if(strBlocoJson == null || strBlocoJson.isEmpty()){
                logger.error("[validarBloco] Bloco esta null");
                return false;
            }
            BlocoJson blocoJson = pilacoinDataHandler.strParaObjBlocoJson(strBlocoJson);

            if (blocoJson.getNonce() == null || blocoJson.getNomeUsuarioMinerador() == null || blocoJson.getChaveUsuarioMinerador() == null) {
                logger.error("[validarBloco] Bloco com atributos obrigatórios null. Ignorando validação. Retornando false para reenviar para a fila 'bloco-minerado'.");
                return false;
            } else if ("Augusto".equals(blocoJson.getNomeUsuarioMinerador())) {
                // Se o criador é "Augusto", reenvia para a fila "pila-minerado"
                logger.warn("[validarBloco] Bloco criado por iris_augusto. Ignorando validação. Retornando false para reenviar para a fila 'bloco-minerado'.");
                return false;
            } else {
                BigInteger hashBigInt = pilacoinDataHandler.getHashBigInteger(strBlocoJson); // HASH em BigInteger gerando da string original
                Dificuldade ultimaDificuldade = dificuldadeService.getUltimaDificuldade();
                if (ultimaDificuldade == null || ultimaDificuldade.getValidadeFinal() == null){
                    logger.error("[validarBloco] Não pode validar Bloco -- ultimaDificuldade é null ou sem validade final..");
                    return false;
                }
                if (!dificuldadeService.estaValida()){
                    logger.error("[validarBloco] Não pode validar Bloco --ultimaDificuldade não está valida.");
                    return false;
                }
                if (hashBigInt.compareTo(ultimaDificuldade.getDificuldade()) < 0) {
                    logger.info("[processarPilacoin] Bloco validado, gerando assinatura.. Usuário minerador: {}", blocoJson.getNomeUsuarioMinerador());
                    byte[] assinaturaBloco = pilacoinDataHandler.gerarAssinatura(blocoJson, rsaKeyPairGenerator.generateOrLoadKeyPair().getPrivate()); //assinatura do obj blocoJson
                    BlocoValidadoJson blocoValidadoJson = new BlocoValidadoJson("iris_augusto", rsaKeyPairGenerator.generateOrLoadKeyPair().getPublic().getEncoded(), assinaturaBloco, blocoJson);
                    //logger.info("[validarBloco] Bloco assinado JSON: {}", pilacoinDataHandler.blocoValidadoJsonParaStrJson(blocoValidadoJson));
                    rabbitMQService.enviarMensagemParaFila("bloco-validado", pilacoinDataHandler.objParaStringJson(blocoValidadoJson));
                    return true;
                }
            }
            //logger.warn("[validarBloco] HASH não é menor que dificuldade... Usuário minerador: {}", blocoJson.getNomeUsuarioMinerador());
            return false;
        } catch (Exception e) {
            logger.error("[processarPilacoin] Erro ao processar Pilacoin: {}", e.getMessage());
            // Em caso de erro, trata de alguma forma (log, lançar exceção, etc.)
            // Pode ser necessário ajustar conforme sua necessidade
            return false;  // Pilacoin não validado devido a erro
        }
    }
}