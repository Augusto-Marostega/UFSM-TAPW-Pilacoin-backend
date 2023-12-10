package br.ufsm.csi.tapw.pilacoin.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonDeserialize
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder(alphabetic = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryJson {
    private Integer idQuery;
    private String nomeUsuario;
    private String tipoQuery;   //PILA, BLOCO ou USUARIOS
    private String status;
    private String nonce;
    private String usuarioMinerador;
    private Long idBloco;
}
