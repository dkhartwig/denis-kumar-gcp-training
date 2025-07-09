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
import com.hartwig.miniwe.MiniWorkflowEngine
import com.hartwig.miniwe.gcloud.storage.GcloudStorage
import com.hartwig.miniwe.kubernetes.BlockingKubernetesClient
import com.hartwig.miniwe.kubernetes.KubernetesStageFactory
import com.hartwig.miniwe.miniwdl.DefinitionSerde
import com.hartwig.miniwe.miniwdl.ExecutionDefinition
import com.hartwig.miniwe.miniwdl.ImmutableResourceProfiles
import com.hartwig.miniwe.miniwdl.WorkflowDefinition
import com.hartwig.miniwe.workflow.WorkflowUtil
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


//class Main {
//
//    fun start() {
//
//
//        val stageFactory = KubernetesStageFactory(
//            "pubsub-workload-sa",
//            "default",
//            BlockingKubernetesClient(),
//            ImmutableResourceProfiles.builder().build()
//        )
//        val storage = StorageOptions.newBuilder().setProjectId("actin-training").build().service
//
//        val gcloudStorage = GcloudStorage(storage, "europe-west4", "actin")
//        val engine = MiniWorkflowEngine(gcloudStorage, stageFactory)
//
//        val workflowDefinition = createWorkflow(emptyMap(), resource)
//        addWorkflow(engine, workflowDefinition)
//
//        val executionDefinition = ExecutionDefinition.builder()
//            .workflow("dk-training-minimwe")
//            .name("whynot")
//            .version("0.0.1")
//            .params(emptyMap())
//            .build()
//
//        engine.getWorkflow(workflowDefinition.id).orElseGet { engine.createWorkflow(workflowDefinition) }
//            .getOrCreateExecution(executionDefinition)
//            .start()
//            .thenApply { success -> {
//                println("Success!")
//            } }
//            .get()
//
//        CountDownLatch(1).await()
//    }
//
//    private fun createWorkflow(
//        definitionParams: Map<String, String>,
//        url: URL?
//    ): WorkflowDefinition = WorkflowUtil.substitute(
//        DefinitionSerde.INSTANCE.readWorkflow(url?.openStream()), definitionParams, emptyMap()
//    )
//
//    private fun addWorkflow(engine: MiniWorkflowEngine, workflowDefinition: WorkflowDefinition?) {
//        val workflow = engine.createWorkflow(workflowDefinition) {
//            println("Hi!! Its done...")
//        }
//        for (workflowExecution in workflow.loadCachedExecutions()) {
//            println("Execution ${workflowExecution.runName} had been interrupted and must be restarted via its invocation event.")
//            workflow.deleteExecution(workflowExecution, true)
//        }
//    }
//}
//
//class NamespacedBucket(private val bucket: String, private val namespace: String? = null) {
//    fun bucketName() = if (namespace == null) bucket else "$bucket-$namespace"
//}

class Main() {
    fun run() {
        val resource = Main::javaClass.javaClass.getResource("/pipeline.yaml")
        val pipe = SubPipelineFactory(resource)
        pipe.launch()
    }
}

fun main() {
    Main().run()
}

//fun main(args: Array<String>) {
//    val storage: Storage = StorageOptions.newBuilder()
//        .setProjectId(projectId)
//        .build()
//        .service
//    val messageReceiver = CustomMessageReceiver(storage)
//    subscribeAsyncExample(messageReceiver)
//}
//
//fun subscribeAsyncExample(receiver: MessageReceiver) {
//    val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)
//    var subscriber: Subscriber? = null
//    try {
//        subscriber = Subscriber.newBuilder(subscriptionName, receiver).build()
//        Runtime.getRuntime().addShutdownHook(Thread {
//            subscriber.stopAsync().awaitTerminated(2, TimeUnit.SECONDS)
//        })
//        subscriber.startAsync().awaitRunning()
//        println("Listening for messages on $subscriptionName ...")
//        subscriber.awaitTerminated()
//    } catch (_: TimeoutException) {
//        subscriber?.stopAsync()
//    }
//}
//
//class CustomMessageReceiver(
//    private val storage: Storage
//) : MessageReceiver {
//    override fun receiveMessage(message: PubsubMessage?, consumer: AckReplyConsumer?) {
//        val data = message?.data?.toStringUtf8() ?: ""
//        println("Received message ${message?.messageId}: $data with ID = ${message?.messageId}")
//        val blobId = BlobId.of(bucketName, objectName)
//        val blobInfo = BlobInfo.newBuilder(blobId).build()
//        storage.create(blobInfo, data.toByteArray(Charsets.UTF_8))
//        consumer?.ack()
//    }
//}