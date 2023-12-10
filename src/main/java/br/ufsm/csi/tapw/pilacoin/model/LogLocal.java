package br.ufsm.csi.tapw.pilacoin.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "log_local")
public class LogLocal {
    @Id
    @GeneratedValue
    @Column(name = "log_id", unique = true)
    private BigInteger id;
    @Column(name = "tipo")
    private String tipo;
    @Column(name = "status")
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @Column(name = "data_criacao")
    private Date dataCriacao;
    @Column(name = "conteudo", length = 4096)
    private String conteudo;

}
