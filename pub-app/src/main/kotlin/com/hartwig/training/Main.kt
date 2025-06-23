package com.hartwig.training

import com.google.api.core.ApiFuture
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.TopicName
import java.util.concurrent.TimeUnit

fun main() {
    val projectId = "actin-training"
    val topicId = "gcp-training-denis"
    publisherExample(projectId, topicId)
}

fun publisherExample(projectId: String?, topicId: String?) {
    val topicName = TopicName.of(projectId, topicId)
    var publisher: Publisher? = null
    try {
        publisher = Publisher.newBuilder(topicName).build()
        val message = "Hello World!"
        val data = ByteString.copyFromUtf8(message)
        val pubsubMessage = PubsubMessage.newBuilder().setData(data).build()
        val messageIdFuture: ApiFuture<String?> = publisher.publish(pubsubMessage)
        val messageId = messageIdFuture.get()
        println("Published message ID: $messageId")
    } finally {
        publisher?.let {
            it.shutdown()
            it.awaitTermination(1, TimeUnit.MINUTES)
        }
    }
}