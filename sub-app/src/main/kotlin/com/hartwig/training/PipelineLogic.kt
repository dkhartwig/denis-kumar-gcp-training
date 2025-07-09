package com.hartwig.training

import com.hartwig.miniwe.MiniWorkflowEngine
import com.hartwig.miniwe.miniwdl.ExecutionDefinition
import com.hartwig.miniwe.miniwdl.WorkflowDefinition

class PipelineLogic(
    private val engine: MiniWorkflowEngine,
    private val workflowDefinition: WorkflowDefinition,
    private val executionDefinition: ExecutionDefinition,
) {

    /**
     * 1) Pull any from bucket to runtime bucket
     * 2) Run workflow (for now only 'bar mem')
     * 3) Delete pulled file from runtime bucket
     * 4) Upload the result to bucket
     */
    fun run() {
        println("Let's start....")
        engine.getWorkflow(workflowDefinition.id).orElseGet { engine.createWorkflow(workflowDefinition) }
            .getOrCreateExecution(executionDefinition)
            .start()
            .thenApply { success -> {
                println("Success!")
            } }
            .get()
    }
}