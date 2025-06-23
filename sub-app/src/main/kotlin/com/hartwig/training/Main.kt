package com.hartwig.training

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


private const val projectId = "actin-training"
private const val subscriptionId = "gcp-training-denis-subscription"
private const val bucketName = "gcp-training-denis"
private const val objectName = "hello-world.txt"

fun main(args: Array<String>) {
    val storage: Storage = StorageOptions.newBuilder()
        .setProjectId(projectId)
        .build()
        .service
    val messageReceiver = CustomMessageReceiver(storage)
    subscribeAsyncExample(messageReceiver)
}

fun subscribeAsyncExample(receiver: MessageReceiver) {
    val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)
    var subscriber: Subscriber? = null
    try {
        subscriber = Subscriber.newBuilder(subscriptionName, receiver).build()
        Runtime.getRuntime().addShutdownHook(Thread {
            subscriber.stopAsync().awaitTerminated(2, TimeUnit.SECONDS)
        })
        subscriber.startAsync().awaitRunning()
        println("Listening for messages on $subscriptionName ...")
        subscriber.awaitTerminated()
    } catch (_: TimeoutException) {
        subscriber?.stopAsync()
    }
}

class CustomMessageReceiver(
    private val storage: Storage
) : MessageReceiver {
    override fun receiveMessage(message: PubsubMessage?, consumer: AckReplyConsumer?) {
        val data = message?.data?.toStringUtf8() ?: ""
        println("Received message ${message?.messageId}: $data with ID = ${message?.messageId}")
        val blobId = BlobId.of(bucketName, objectName)
        val blobInfo = BlobInfo.newBuilder(blobId).build()
        storage.create(blobInfo, data.toByteArray(Charsets.UTF_8))
        consumer?.ack()
    }
}