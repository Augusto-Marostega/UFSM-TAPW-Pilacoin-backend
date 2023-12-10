package br.ufsm.csi.tapw.pilacoin.controller;

import br.ufsm.csi.tapw.pilacoin.model.PilacoinServer;
import br.ufsm.csi.tapw.pilacoin.model.Usuario;
import br.ufsm.csi.tapw.pilacoin.model.json.QueryJson;
import br.ufsm.csi.tapw.pilacoin.model.json.TransacaoJson;
import br.ufsm.csi.tapw.pilacoin.model.json.TransferirPilaInfo;
import br.ufsm.csi.tapw.pilacoin.repository.PilacoinServerRepository;
import br.ufsm.csi.tapw.pilacoin.repository.UsuarioRepository;
import br.ufsm.csi.tapw.pilacoin.service.MinerarPilacoinService;
import br.ufsm.csi.tapw.pilacoin.service.RabbitMQService;
import br.ufsm.csi.tapw.pilacoin.service.TransferirPilacoinService;
import br.ufsm.csi.tapw.pilacoin.service.UsuarioService;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/pilacoin")
public class PilacoinController {

    private final MinerarPilacoinService minerarPilaCoinService;
    private final TransferirPilacoinService transferirPilacoinService;
    private final PilacoinDataHandler pilacoinDataHandler;
    private final RabbitMQService rabbitMQService;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PilacoinServerRepository pilacoinServerRepository;

    @Autowired
    public PilacoinController(MinerarPilacoinService minerarPilaCoinService, TransferirPilacoinService transferirPilacoinService, PilacoinDataHandler pilacoinDataHandler, RabbitMQService rabbitMQService, UsuarioService usuarioService, UsuarioRepository usuarioRepository, PilacoinServerRepository pilacoinServerRepository) {
        this.minerarPilaCoinService = minerarPilaCoinService;
        this.transferirPilacoinService = transferirPilacoinService;
        this.pilacoinDataHandler = pilacoinDataHandler;
        this.rabbitMQService = rabbitMQService;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.pilacoinServerRepository = pilacoinServerRepository;
    }

    @GetMapping("/usuario/atualizar") //atualiza os usuários com base no servidor.
    public String atualizarUsuarios() {
        usuarioService.queryAtualizarUsuarios();
        return "Atualizando usuários...";
    }

    @GetMapping("/query/atualizarpilas") //vai atualizar a tabela pilas com base no servidor
    public String getAtualizarPilas(){
        QueryJson queryJson = QueryJson.builder()
                .idQuery(209)
                .nomeUsuario("iris_augusto")
                .tipoQuery("PILA")
                .usuarioMinerador("iris_augusto")
                .build();
        rabbitMQService.enviarMensagemParaFila("query", pilacoinDataHandler.objParaStringJson(queryJson));
        return "Tabela de iris_augusto atualizada : " + "\n queryJson: " + queryJson;
    }

    @GetMapping("/query/atualizarblocos") //vai atualiar a tabela pilas com base no servidor AINDA NÃO FUNCIONA
    public String getAtualizarBlocos(){
        QueryJson queryJson = QueryJson.builder()
                .idQuery(309)
                .nomeUsuario("iris_augusto")
                .tipoQuery("BLOCO")
                //.usuarioMinerador("iris_augusto")
                .build();

        System.out.println("TESTANNDOOO");
        rabbitMQService.enviarMensagemParaFila("query", pilacoinDataHandler.objParaStringJson(queryJson));
        return "Tabela de iris_augusto atualizada : " + "\n queryJson: " + queryJson;
    }

    @GetMapping("/meuspilas")
    public List<PilacoinServer> getMeusPilas(){
        List<PilacoinServer> pilas = pilacoinServerRepository.findAll();
        return pilas;
    }

    @GetMapping("/usuarios")
    public List<Usuario>  getUsuarios(){
        List<Usuario> usuarios = usuarioRepository.findAll();
        return usuarios;
    }

    @PostMapping("/transferir")
    public ResponseEntity<String> postTransferirPila(@RequestBody TransferirPilaInfo transferirPilaInfo) throws IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        Boolean transacaoOk = false;

        Optional<Usuario> usuario = usuarioRepository.findById(transferirPilaInfo.getIdUsuarioDestino());
        if (usuario.isPresent()) {
            transferirPilacoinService.transferirPilacoinAugusto(transferirPilaInfo.getNoncePila(), usuario.get().getChavePublica(), usuario.get().getNome());
            transacaoOk = true;
        } else {
            transacaoOk = false;
            // Tratamento para quando o usuário não é encontrado
            //return "Usuário não encontrado";
        }

        if(transacaoOk){
            return ResponseEntity.status(HttpStatus.OK).body("Transação realizada com sucesso!");
        }
        else {
            return ResponseEntity.status(HttpStatus.OK).body("Erro ao transferir!");
        }
    }

    @GetMapping("/query/pilacoinbyminerador/{usuarioMinerador}") // funciona apenas para testes na back end
    public String getPilaByMinerador(@PathVariable String usuarioMinerador){
        QueryJson queryJson = QueryJson.builder()
                .idQuery(202)
                .nomeUsuario("iris_augusto")
                .tipoQuery("PILA")
                .usuarioMinerador(usuarioMinerador)
                .build();

        rabbitMQService.enviarMensagemParaFila("query", pilacoinDataHandler.objParaStringJson(queryJson));
        return "Get Pilacoin by usuarioMinerador: " + usuarioMinerador + "\n queryJson: " + queryJson;
    }

    @GetMapping("/query/blocobynonce/{nonce}") //não funciona
    public String getBlocoByNonce(@PathVariable String nonce){
        return "Get Bloco by Nonce : " + nonce;
    }

    @GetMapping("/query/usuariobynome/{usuario}") //não funciona
    public String getUsuarioByNome(@PathVariable String usuario)
    {
        return "Get Usuário by Nome: " + usuario;
    }


    @GetMapping("/minerarpilacoin/iniciar")
    public String iniciarPilaCoinMineracao() {
        CompletableFuture<Void> future = minerarPilaCoinService.minerarPilacoinAsync();
        // Retorna uma mensagem indicando que a mineração foi iniciada
        return "Mineração de Pilacoin iniciada. Status: " +
                (future.isDone() ? "Concluída" : "Em andamento");
    }

    @GetMapping("/minerarpilacoin/parar")
    public String pararPilaCoinMineracao() {
        minerarPilaCoinService.pararMineracao();
        // Retorna uma mensagem indicando que a mineração foi parada
        return "Mineração de Pilacoin interrompida.";
    }

    @GetMapping("/gethash/{str}")
    public String getChavePublica(@PathVariable String str){
        BigInteger hashBigInteger = pilacoinDataHandler.getHashBigInteger(str);
        return "Original: " + str + " --Hash: " + hashBigInteger.toString();
    }
}
