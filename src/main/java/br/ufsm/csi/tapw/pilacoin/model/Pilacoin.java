package br.ufsm.csi.tapw.pilacoin.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
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
@Table(name = "pilacoin")
public class Pilacoin  {
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
}
