package br.ufsm.csi.tapw.pilacoin.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pilacoin_server") //tabela que carrega os status de pilas do servidor
public class PilacoinServer {
    @Id
    @Column(name = "nonce", unique = true)
    private String nonce;

    @Column(name = "status")
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @Column(name = "data_criacao")
    private Date dataCriacao;
    @Column(name = "chave_criador")
    private byte[] chaveCriador;
    @Column(name = "nome_criador")
    private String nomeCriador;
    @Column(name = "transferido")
    private String transferido; //sim ou nao
}
