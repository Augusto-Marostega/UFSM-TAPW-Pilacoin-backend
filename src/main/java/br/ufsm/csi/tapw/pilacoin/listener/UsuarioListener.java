package br.ufsm.csi.tapw.pilacoin.listener;

import br.ufsm.csi.tapw.pilacoin.model.Pilacoin;
import br.ufsm.csi.tapw.pilacoin.model.PilacoinServer;
import br.ufsm.csi.tapw.pilacoin.model.Usuario;
import br.ufsm.csi.tapw.pilacoin.model.json.MsgJson;
import br.ufsm.csi.tapw.pilacoin.model.json.QueryRespostaJson;
import br.ufsm.csi.tapw.pilacoin.repository.PilacoinServerRepository;
import br.ufsm.csi.tapw.pilacoin.repository.PilacoinRepository;
import br.ufsm.csi.tapw.pilacoin.repository.UsuarioRepository;
import br.ufsm.csi.tapw.pilacoin.service.RabbitMQService;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class UsuarioListener {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioListener.class);

    private final RabbitMQService rabbitMQService;
    private final PilacoinRepository pilacoinRepository;
    private final PilacoinDataHandler pilacoinDataHandler;
    private final UsuarioRepository usuarioRepository;
    private final PilacoinServerRepository pilacoinServerRepository;

    @Autowired
    public UsuarioListener(RabbitMQService rabbitMQService,
                           PilacoinRepository pilacoinRepository,
                           PilacoinDataHandler pilacoinDataHandler,
                           UsuarioRepository usuarioRepository,
                           PilacoinServerRepository pilacoinServerRepository) {
        this.rabbitMQService = rabbitMQService;
        this.pilacoinRepository = pilacoinRepository;
        this.pilacoinDataHandler = pilacoinDataHandler;
        this.usuarioRepository = usuarioRepository;
        this.pilacoinServerRepository = pilacoinServerRepository;
    }


    @RabbitListener(queues = "iris_augusto")
    public void handleIrisAugustoMessage(String mensagem) throws JsonProcessingException {
        //System.out.println(mensagem);
        MsgJson msgJson = new ObjectMapper().readValue(mensagem, MsgJson.class);
        if (msgJson.getErro() != null){
            logger.error("[handleIrisAugustoMessage] Fila 'iris_augusto': {}", mensagem);
        } else if (msgJson.getMsg() != null && msgJson.getMsg().contains("AG_BLOCO") && !msgJson.getMsg().contains("Validacao ok. Pila esta AG_BLOCO.") ){
            logger.warn("[handleIrisAugustoMessage] Fila 'iris_augusto': {}", mensagem);
            //precisa salvar no banco de dados o pila (AG_BLOCO = aguardando bloco)
            Pilacoin pilacoinBD = pilacoinRepository.findByNonce(msgJson.getNonce());
            if(pilacoinBD != null){
                pilacoinBD.setStatus("AG_BLOCO");
                pilacoinRepository.saveAndFlush(pilacoinBD);
                logger.info("[handleIrisAugustoMessage] Atualizando status 'AG_BLOCO' para o pila: {}", pilacoinBD.getNonce());
            } else {
                logger.info("[handleIrisAugustoMessage] Não foi possível encontrar o pila para atualziar o status 'AG_BLOCO' ");
            }
        } else {
            logger.info("[handleIrisAugustoMessage] Recebido da fila 'iris_augusto': {}", mensagem);
        }
    }
    @RabbitListener(queues = "iris_augusto-query")
    public void handleIrisAugustoQueryMessage(String mensagem) {
        logger.info("[handleAugustoQueryMessage] Recebido da fila 'iris_augusto-query': {}", mensagem);
        if(mensagem == null || mensagem.isEmpty()){
            logger.warn("[handleAugustoQueryMessage] 'iris_augusto-query' vazio...");
            return;
        }
        QueryRespostaJson queryRespostaJson = pilacoinDataHandler.strParaObjQueryResposta(mensagem);
        if(queryRespostaJson.getUsuario() != null && !queryRespostaJson.getUsuario().isEmpty() && !queryRespostaJson.getUsuario().equals("iris_augusto")){
            logger.warn("[handleIrisAugustoQueryMessage] 'iris_augusto-query' Usuário não é 'iris_augusto'...");
        }
        if(queryRespostaJson.getPilasResult() != null && !queryRespostaJson.getPilasResult().isEmpty()){
            if(queryRespostaJson.getIdQuery().equals(209)){ //query 209 para atualizar os pilas do usuário iris_augusto do banco de dados...
                logger.info("[handleIrisAugustoQueryMessage] Query do tipo 'PILA' idQuery 209...");
                List<PilacoinServer> pilacoinList = pilacoinDataHandler.pilacoinJsonListParaPilacoin(queryRespostaJson.getPilasResult());
                List<PilacoinServer> pilacoinListAtual = pilacoinServerRepository.findAll();

                List<PilacoinServer> itemsToUpdate = pilacoinListAtual.stream()
                        .filter(itemAtual -> {
                            // Encontrar o item correspondente na lista pilacoinList com o mesmo ID
                            PilacoinServer correspondente = pilacoinList.stream()
                                    .filter(item -> Objects.equals(item.getNonce(), itemAtual.getNonce())) // Supondo que 'id' é o identificador
                                    .findFirst()
                                    .orElse(null);

                            // Atualizar o item se o 'transferido' for nulo na lista pilacoinList
                            return correspondente != null && correspondente.getTransferido() == null;
                        })
                        .collect(Collectors.toList());

                // Atualizar os itens correspondentes com 'transferido' nulo na lista pilacoinListAtual
                for (PilacoinServer item : itemsToUpdate) {
                    // Atualizar o atributo 'transferido' ou qualquer outro atributo que seja necessário
                    //item.setTransferido(/* Defina o valor adequado aqui */);
                }

                // Salvar as alterações no banco de dados
                if (!itemsToUpdate.isEmpty()) {
                    pilacoinServerRepository.saveAll(itemsToUpdate);
                }

                //pilacoinServerRepository.saveAllAndFlush(pilacoinList);
            }
        }
        if(queryRespostaJson.getBlocosResult() != null && !queryRespostaJson.getBlocosResult().isEmpty()){
            if(queryRespostaJson.getIdQuery().equals(309)){ //query 209 para atualizar os pilas do usuário iris_augusto do banco de dados...
                logger.info("[handleIrisAugustoQueryMessage] Query do tipo 'BLOCO' idQuery 309...");
                //List<PilacoinServer> pilacoinList = pilacoinDataHandler.pilacoinJsonListParaPilacoin(queryRespostaJson.getPilasResult());
                //pilacoinServerRepository.saveAllAndFlush(pilacoinList);
            }

        }
        if(queryRespostaJson.getUsuariosResult() != null && !queryRespostaJson.getUsuariosResult().isEmpty()){
            logger.info("[handleIrisAugustoQueryMessage] Query do tipo 'USUARIOS'...");
            if(queryRespostaJson.getIdQuery().equals(101)){ //query 101 para atualizar usuários do banco de dados...
                logger.info("[handleIrisAugustoQueryMessage] Query do tipo 'USUARIOS' idQuery 101...");
                List<Usuario> usuarioList = pilacoinDataHandler.usuarioJsonListParaUsuario(queryRespostaJson.getUsuariosResult());
                usuarioRepository.saveAllAndFlush(usuarioList);
            }
        }
    }

    @RabbitListener(queues = "report")
    public void handleReport(String mensagem){
        logger.info("[handleReport] Recebido na fila 'report': {}", mensagem);
    }

    @RabbitListener(queues = "clients-errors")
    public void handleClientsErrors(String mensagem){
        logger.warn("[handleClientsErrors] fila 'clients-errors': {}", mensagem);
    }
}
