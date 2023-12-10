package br.ufsm.csi.tapw.pilacoin.service;

import br.ufsm.csi.tapw.pilacoin.model.LogLocal;
import br.ufsm.csi.tapw.pilacoin.repository.LogLocalRepository;
import br.ufsm.csi.tapw.pilacoin.util.PilacoinDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class RabbitMQService {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQService.class);
    private final AmqpTemplate amqpTemplate;
    private final LogLocalRepository logLocalRepository;
    private final PilacoinDataHandler pilacoinDataHandler;
    @Autowired
    public RabbitMQService(AmqpTemplate amqpTemplate, LogLocalRepository logLocalRepository, PilacoinDataHandler pilacoinDataHandler) {
        this.amqpTemplate = amqpTemplate;
        this.logLocalRepository = logLocalRepository;
        this.pilacoinDataHandler = pilacoinDataHandler;
    }

    public void enviarMensagemParaFila(String nomeFila, String mensagem) {
        logger.info("[enviarMensagemParaFila] Mensagem enviada fila: '{}', mensagem: {}", nomeFila, mensagem);
        amqpTemplate.convertAndSend(nomeFila, mensagem);
        LogLocal loglocal = LogLocal.builder()
                .tipo("rabbitmq_enviar_msg")
                .dataCriacao(new Date())
                .status("info")
                .conteudo("Mensagem enviada fila: " + pilacoinDataHandler.reduzirString(nomeFila) + " mensagem: " + pilacoinDataHandler.reduzirString(mensagem))
                .build();
        logLocalRepository.saveAndFlush(loglocal);
    }
}
