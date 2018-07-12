package bitkonnekt

import org.apache.kafka.clients.producer.*
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import com.neovisionaries.ws.client.*
import io.github.cdimascio.dotenv.dotenv
import com.google.gson.GsonBuilder


val BOOTSTRAP_SERVERS = "localhost:9092"
val SNAPPY_FLAG = "snappy"


fun createProducer(
        bootstrap_servers: String,
        compression: String,
        client_id: String
): Producer<Nothing?, String> {
    var props = Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, client_id)
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compression)
    props.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer::class.java.name
    )
    props.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            StringSerializer::class.java.name
    )

    return KafkaProducer(props)
}

class gdaxWebsocket(
        subscribe_msg: String,
        topic: String,
        producer: Producer<Nothing?, String>
) {
    val GDAX_WSS_URL = "wss://ws-feed.prime.coinbase.com"
    val factory = WebSocketFactory()
    val ws = factory.createSocket(GDAX_WSS_URL, 5000)

    init {
        ws.addListener(object: WebSocketAdapter() {

            override fun onConnected(
                    websocket: WebSocket,
                    headers: Map<String, List<String>>
            ) {
                websocket.sendText(subscribe_msg)
            }

            override fun onTextMessage(
                    websocket: WebSocket,
                    message: String
            ) {
                val record: ProducerRecord<Nothing?, String> = ProducerRecord(topic, null, message)
                producer.send(record, {
                    metadata, exception ->
                    if (metadata == null) {
                        exception.printStackTrace()
                    }
                })
            }
        })
    }

    fun connect() {
        ws.connect()
    }
}

data class GdaxSubscribeMessage(
        val type: String,
        val product_ids: List<String>,
        val channels: List<String>
)

fun subscribeMessage(
        productsStr: String,
        channelsStr: String
): GdaxSubscribeMessage {
    val type = "subscribe"
    var prodcuts: List<String> = productsStr.split(",").map {
        it.trim().toUpperCase()
    }
    var channels: List<String> = channelsStr.split(",").map {
        it.trim().toLowerCase()
    }
    return GdaxSubscribeMessage(type, prodcuts, channels)
}

fun subscribeJson(
        prodcutsStr: String,
        channelsStr: String
): String {
	val gson = GsonBuilder().create()
    return gson.toJson(subscribeMessage(prodcutsStr, channelsStr))
}

fun main(args: Array<String>) {
    val dotenv = dotenv {
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    val producer = createProducer(
            dotenv["GDAX_KAFKA_SERVERS"] ?: "localhost:9092",
            dotenv["GDAX_KAFKA_COMPRESSION"] ?: "none",
            dotenv["GDAX_KAFKA_CLIENT_ID"] ?: "GDAX"
    )

    val gdax_subscribe = subscribeJson(
            dotenv["GDAX_PRODUCTS"] ?: "BTC-USD",
            dotenv["GDAX_CHANNELS"] ?: "ticker"
    )

    val topic = dotenv["GDAX_KAFKA_TOPIC"] ?: "testing"
    val ws = gdaxWebsocket(gdax_subscribe, topic, producer)
    ws.connect()
}
