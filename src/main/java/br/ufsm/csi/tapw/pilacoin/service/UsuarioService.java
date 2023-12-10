package br.ufsm.csi.tapw.pilacoin.service;

import br.ufsm.csi.tapw.pilacoin.model.json.QueryJson;
import br.ufsm.csi.tapw.pilacoin.repository.UsuarioRepository;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import br.ufsm.csi.tapw.pilacoin.util.RSAKeyPairGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final RSAKeyPairGenerator rsaKeyPairGenerator;
    private final RabbitMQService rabbitMQService;
    private final PilacoinDataHandler pilacoinDataHandler;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UsuarioService(
            PilacoinDataHandler pilacoinDataHandler,
            RSAKeyPairGenerator rsaKeyPairGenerator,
            RabbitMQService rabbitMQService,
            UsuarioRepository usuarioRepository) {
        this.pilacoinDataHandler = pilacoinDataHandler;
        this.rsaKeyPairGenerator = rsaKeyPairGenerator;
        this.rabbitMQService = rabbitMQService;
        this.usuarioRepository = usuarioRepository;
    }
    public void queryAtualizarUsuarios(){   //idQuery 101 #solicitação dos usuários para atualizar os usuários do banco de dados.
        String strQuery = "";
        QueryJson queryJson = QueryJson.builder()
                .idQuery(101)
                .nomeUsuario("iris_augusto")
                .tipoQuery("USUARIOS")
                .build();
        strQuery = pilacoinDataHandler.objParaStringJson(queryJson);
        logger.info("[queryAtualizarUsuarios] Enviando query para atualizar usuários: {}", strQuery);
        rabbitMQService.enviarMensagemParaFila("query", strQuery);
    }
    public void atualizarUsuariosDB(){
        
        logger.info("[atualizarUsuarios] Tentando atualizar usuários...");
    }
}
