package io.seldon.apife.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.seldon.protos.PredictionProtos.PredictionRequestResponseDef;

@Component
public class KafkaRequestResponseProducer {
	
	private static Logger logger = LoggerFactory.getLogger(KafkaRequestResponseProducer.class.getName());
    final private static String ENV_VAR_SELDON_KAFKA_SERVER = "SELDON_ENGINE_KAFKA_SERVER";

    
    private KafkaProducer<String, PredictionRequestResponseDef > producer;
    
    private boolean enabled = false;
    
    @Autowired
	public KafkaRequestResponseProducer(@Value("${seldon.kafka.enable}") boolean kafkaEnabled) 
	{
		if (kafkaEnabled)
		{
			enabled = true;
			String kafkaHostPort = System.getenv(ENV_VAR_SELDON_KAFKA_SERVER);
	        logger.info(String.format("using %s[%s]", ENV_VAR_SELDON_KAFKA_SERVER, kafkaHostPort));
	        if (kafkaHostPort == null) {
	            logger.warn("*WARNING* SELDON_KAFKA_SERVER environment variable not set!");
	            kafkaHostPort = "localhost:9093";
	        }
	        logger.info("Starting kafka client with server "+kafkaHostPort);
		    Properties props = new Properties();
		    props.put("bootstrap.servers", kafkaHostPort);
		    props.put("client.id", "RequestResponseProducer");
		    props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "1000");
		    props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, "1000");
		    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG,"20"); //NB need to investigate issues of Kafka not able to get metadata
		    producer = new KafkaProducer<>(props, new StringSerializer(), new PredictionRequestResponseSerializer());
		}
		else
			logger.warn("Kafka not enabled");
	}
	
	public void send(String clientId, PredictionRequestResponseDef data)
	{
		if (enabled)
			producer.send(new ProducerRecord<>(clientId,
					data.getResponse().getMeta().getPuid(),
					data));
	}
	
}


