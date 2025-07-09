package com.hartwig.training

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.cloud.storage.StorageOptions
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.hartwig.miniwe.MiniWorkflowEngine
import com.hartwig.miniwe.gcloud.storage.GcloudStorage
import com.hartwig.miniwe.kubernetes.BlockingKubernetesClient
import com.hartwig.miniwe.kubernetes.KubernetesStageFactory
import com.hartwig.miniwe.miniwdl.DefinitionSerde
import com.hartwig.miniwe.miniwdl.ExecutionDefinition
import com.hartwig.miniwe.miniwdl.ImmutableResourceProfiles
import com.hartwig.miniwe.miniwdl.WorkflowDefinition
import com.hartwig.miniwe.workflow.WorkflowUtil
import io.grpc.netty.shaded.io.netty.handler.timeout.TimeoutException
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val projectId = "actin-training"
private const val subscriptionId = "gcp-training-denis-subscription"
private const val bucketName = "gcp-training-denis"
private const val objectName = "hello-world.txt"

class SubPipelineFactory(
    private val resource: URL?
) {

    fun launch() {
        val stageFactory = KubernetesStageFactory(
            "pubsub-workload-sa",
            "default",
            BlockingKubernetesClient(),
            ImmutableResourceProfiles.builder().build()
        )
        val storage = StorageOptions.newBuilder().setProjectId("actin-training").build().service

        val gcloudStorage = GcloudStorage(storage, "europe-west4", "actin")
        val engine = MiniWorkflowEngine(gcloudStorage, stageFactory)

        val workflowDefinition = createWorkflow(emptyMap(), resource)
        addWorkflow(engine, workflowDefinition)

        val executionDefinition = ExecutionDefinition.builder()
            .workflow("dk-training-minimwe")
            .name("whynot")
            .version("0.0.1")
            .params(emptyMap())
            .build()

        val pipelineLogic = PipelineLogic(engine, workflowDefinition, executionDefinition)
        subscribeAsyncExample(pipelineLogic)
        CountDownLatch(1).await()
    }

    private fun subscribeAsyncExample(pipelineLogic: PipelineLogic) {
        val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)
        var subscriber: Subscriber? = null
        try {
            subscriber = Subscriber.newBuilder(subscriptionName, MessageReceiver { p0, p1 ->
                val data = p0?.data?.toStringUtf8() ?: ""
                println("Received message ${p0?.messageId}: $data with ID = ${p0?.messageId}")
                p1?.ack()
                pipelineLogic.run()
            }).build()
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

    private fun createWorkflow(
        definitionParams: Map<String, String>,
        url: URL?
    ): WorkflowDefinition = WorkflowUtil.substitute(
        DefinitionSerde.INSTANCE.readWorkflow(url?.openStream()), definitionParams, emptyMap()
    )

    private fun addWorkflow(engine: MiniWorkflowEngine, workflowDefinition: WorkflowDefinition?) {
        val workflow = engine.createWorkflow(workflowDefinition)
        for (workflowExecution in workflow.loadCachedExecutions()) {
            println("Execution ${workflowExecution.runName} had been interrupted and must be restarted via its invocation event.")
            workflow.deleteExecution(workflowExecution, true)
        }
    }
}

class CustomMessageReceiver(

) : MessageReceiver {
    override fun receiveMessage(message: PubsubMessage?, consumer: AckReplyConsumer?) {
        val data = message?.data?.toStringUtf8() ?: ""
        println("Received message ${message?.messageId}: $data with ID = ${message?.messageId}")
        consumer?.ack()
    }
}