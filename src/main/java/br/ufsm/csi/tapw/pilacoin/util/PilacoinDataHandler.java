package br.ufsm.csi.tapw.pilacoin.util;

import br.ufsm.csi.tapw.pilacoin.model.Pilacoin;
import br.ufsm.csi.tapw.pilacoin.model.PilacoinServer;
import br.ufsm.csi.tapw.pilacoin.model.Usuario;
import br.ufsm.csi.tapw.pilacoin.model.json.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class PilacoinDataHandler {

    private static final Logger logger = LoggerFactory.getLogger(PilacoinDataHandler.class);

    private final ObjectMapper objectMapper;

    public PilacoinDataHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BigInteger getHashBigInteger(Object o) {
        try {
            String jsonString = null;
            if (o instanceof String){
                jsonString = (String) o;
            } else {
                ObjectMapper om = new ObjectMapper();
                jsonString = om.writeValueAsString(o);
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return new BigInteger(md.digest(jsonString.getBytes(StandardCharsets.UTF_8))).abs();
        } catch (Exception e) {
            logger.error("[getHashBigInteger] Erro ao gerar hash para objeto", e);
            return null;
        }
    }

    public byte[] getHashByteArr(Object o) {
        try {
            String jsonString = null;
            if (o instanceof String){
                jsonString = (String) o;
            } else {
                ObjectMapper om = new ObjectMapper();
                jsonString = om.writeValueAsString(o);
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(jsonString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("[getHashByteArr] Erro ao gerar hash para objeto", e);
            return null;
        }
    }
    public String objParaStringJson(Object anyObject) {
        try {
            String jsonString = null;
            if (anyObject instanceof String){
                jsonString = (String) anyObject;
            } else {
                ObjectMapper om = new ObjectMapper();
                jsonString = om.writeValueAsString(anyObject);
            }
            return jsonString;
        } catch (Exception e) {
            logger.error("[objParaStringJson] Erro ao converter objeto para String JSON", e);
            return null;
        }
    }

    public byte[] gerarAssinatura(Object anyObject, PrivateKey privateKey) throws IllegalBlockSizeException, BadPaddingException, JsonProcessingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        //String strJson = objectMapper.writeValueAsString(anyObject);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] hash = this.getHashByteArr(anyObject);
        return cipher.doFinal(hash);
    }

    public PilacoinJson strParaObjPilacoinJson(String strJson) {
        try {
            return objectMapper.readValue(strJson, PilacoinJson.class);
        } catch (Exception e) {
            logger.error("[strParaObjPilacoinJson] Erro ao converter JSON para objeto PilacoinJson: {}", strJson, e);
            return null;
        }
    }
    public PilacoinValidadoJson strParaObjPilacoinValidadoJson(String strJson) {
        try {
            return objectMapper.readValue(strJson, PilacoinValidadoJson.class);
        } catch (Exception e) {
            logger.error("[strParaObjPilacoinValidadoJson] Erro ao converter JSON para objeto PilacoinJson: {}", strJson , e);
            return null;
        }
    }
    public BlocoJson strParaObjBlocoJson(String strJson) {
        try {
            return objectMapper.readValue(strJson, BlocoJson.class);
        } catch (Exception e) {
            logger.error("[strParaObjPilacoinJson] Erro ao converter JSON para objeto PilacoinJson: {}", strJson, e);
            return null;
        }
    }
    public BlocoValidadoJson strParaObjBlocoValidadoJson(String strJson) {
        try {
            return objectMapper.readValue(strJson, BlocoValidadoJson.class);
        } catch (Exception e) {
            logger.error("[strParaObjBlocoValidadoJson] Erro ao converter JSON para objeto BlocoValidadoJson: {}", strJson, e);
            return null;
        }
    }
    public TransacaoJson strParaObjTransacaoJson(String strJson){
        try {
            return objectMapper.readValue(strJson, TransacaoJson.class);
        } catch (Exception e) {
            logger.error("[strParaObjBlocoValidadoJson] Erro ao converter JSON para objeto TransacaoJson: {}", strJson, e);
            return null;
        }
    }
    public Pilacoin pilacoinJsonParaPilacoin(PilacoinJson pilacoinJson){
        try{
            Pilacoin pilacoin = Pilacoin.builder()
                    .chaveCriador(pilacoinJson.getChaveCriador())
                    .nomeCriador(pilacoinJson.getNomeCriador())
                    .nonce(pilacoinJson.getNonce())
                    .dataCriacao(pilacoinJson.getDataCriacao())
                    .build();
            return pilacoin;
        } catch (Exception e){
            return null;
        }
    }


    public Usuario usuarioJsonParaUsuario(UsuarioJson usuarioJson){
        try{
            Usuario usuario = Usuario.builder()
                    .id(usuarioJson.getId())
                    .chavePublica(Base64.getDecoder().decode(usuarioJson.getChavePublica()))
                    .nome(usuarioJson.getNome())
                    .build();
            return usuario;
        } catch (Exception e){
            return null;
        }
    }

    public List<Usuario> usuarioJsonListParaUsuario(List<UsuarioJson> usuarioJsonList) {
        if (usuarioJsonList == null) {
            return new ArrayList<>(); // Retorna uma lista vazia se a lista de entrada for nula
        }
        List<Usuario> usuarios = new ArrayList<>();

        for (UsuarioJson usuarioJson : usuarioJsonList) {
            try {
                Usuario usuario = Usuario.builder()
                        .id(usuarioJson.getId())
                        .chavePublica(Base64.getDecoder().decode(usuarioJson.getChavePublica()))
                        .nome(usuarioJson.getNome())
                        .build();
                usuarios.add(usuario);
            } catch (Exception e) {
                // Em caso de erro na conversão de um elemento, pode tratar de outra forma ou não adicionar à lista
            }
        }
        return usuarios;
    }

    public List<PilacoinServer> pilacoinJsonListParaPilacoin(List<PilacoinJson> pilacoinJsonList) {
        if (pilacoinJsonList == null) {
            return new ArrayList<>(); // Retorna uma lista vazia se a lista de entrada for nula
        }
        List<PilacoinServer> pilacoins = new ArrayList<>();

        for (PilacoinJson pilacoinJson : pilacoinJsonList) {
            try {
                PilacoinServer pilacoin = PilacoinServer.builder()
                            .dataCriacao(pilacoinJson.getDataCriacao())
                            .chaveCriador(pilacoinJson.getChaveCriador())
                            .nomeCriador(pilacoinJson.getNomeCriador())
                            .nonce(pilacoinJson.getNonce())
                            .status(pilacoinJson.getStatus())
                            .build();
                pilacoins.add(pilacoin);
            } catch (Exception e) {
                // Em caso de erro na conversão de um elemento, pode tratar de outra forma ou não adicionar à lista
            }
        }
        return pilacoins;
    }

    public QueryRespostaJson strParaObjQueryResposta(String strJson) {
        try {
            return objectMapper.readValue(strJson, QueryRespostaJson.class);
        } catch (Exception e) {
            logger.error("[strParaObjQueryResposta] Erro ao converter JSON para objeto QueryResposta: {}", strJson, e);
            return null;
        }
    }


}
