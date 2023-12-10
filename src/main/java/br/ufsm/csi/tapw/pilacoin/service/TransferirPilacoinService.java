package br.ufsm.csi.tapw.pilacoin.service;

import br.ufsm.csi.tapw.pilacoin.model.PilacoinServer;
import br.ufsm.csi.tapw.pilacoin.model.json.TransacaoJson;
import br.ufsm.csi.tapw.pilacoin.repository.PilacoinRepository;
import br.ufsm.csi.tapw.pilacoin.repository.PilacoinServerRepository;
import br.ufsm.csi.tapw.pilacoin.repository.UsuarioRepository;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import br.ufsm.csi.tapw.pilacoin.util.RSAKeyPairGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class TransferirPilacoinService {
    private static final Logger logger = LoggerFactory.getLogger(TransferirPilacoinService.class);

    private final RSAKeyPairGenerator rsaKeyPairGenerator;
    private final RabbitMQService rabbitMQService;
    private final PilacoinDataHandler pilacoinDataHandler;
    private final UsuarioRepository usuarioRepository;
    private final PilacoinServerRepository pilacoinServerRepository;

    @Autowired
    public TransferirPilacoinService(
            PilacoinDataHandler pilacoinDataHandler,
            RSAKeyPairGenerator rsaKeyPairGenerator,
            RabbitMQService rabbitMQService,
            UsuarioRepository usuarioRepository,
            PilacoinServerRepository pilacoinServerRepository) {
        this.pilacoinDataHandler = pilacoinDataHandler;
        this.rsaKeyPairGenerator = rsaKeyPairGenerator;
        this.rabbitMQService = rabbitMQService;
        this.usuarioRepository = usuarioRepository;
        this.pilacoinServerRepository = pilacoinServerRepository;
    }

    public void transferirPilacoinAugusto(String nonce, byte[] chaveUsuarioDestino, String nomeUsuarioDestino) throws IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        logger.info("[transferirPilacoinAugusto] Nome: {}  Nonce: {}   Chave Usuário Destino: {}",  nomeUsuarioDestino, nonce, chaveUsuarioDestino);
        String nomeUsuarioOrigem = "iris_augusto";
        transferirPilacoin(nonce, rsaKeyPairGenerator.generateOrLoadKeyPair().getPublic().getEncoded(), chaveUsuarioDestino, nomeUsuarioOrigem, nomeUsuarioDestino);
    }

    private void transferirPilacoin(String nonce, byte[] chaveUsuarioOrigem, byte[] chaveUsuarioDestino, String nomeUsuarioOrigem, String nomeUsuarioDestino) throws IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        TransacaoJson pilaTransferir = TransacaoJson.builder()
                .chaveUsuarioOrigem(rsaKeyPairGenerator.generateOrLoadKeyPair().getPublic().getEncoded())
                .chaveUsuarioDestino(chaveUsuarioDestino)
                .nomeUsuarioOrigem(nomeUsuarioOrigem)
                .nomeUsuarioDestino(nomeUsuarioDestino)
                .noncePila(nonce)
                .build();

        String pilaTransferirAssinar = pilacoinDataHandler.objParaStringJson(pilaTransferir);
        byte[] assinaturaPilacoin = pilacoinDataHandler.gerarAssinatura(pilaTransferirAssinar, rsaKeyPairGenerator.generateOrLoadKeyPair().getPrivate());
        pilaTransferir.setAssinatura(assinaturaPilacoin);
        rabbitMQService.enviarMensagemParaFila("transferir-pila", pilacoinDataHandler.objParaStringJson(pilaTransferir));
        PilacoinServer pilaTransferido= pilacoinServerRepository.findByNonce(nonce);
        pilaTransferido.setTransferido("sim");
        pilacoinServerRepository.saveAndFlush(pilaTransferido);
        logger.info("[transferirPilacoin] Pila transferido 'transferir-pila");
    }
}
