package com.r3corda.node.services.events

import com.r3corda.core.contracts.ContractState
import com.r3corda.core.contracts.SchedulableState
import com.r3corda.core.contracts.ScheduledStateRef
import com.r3corda.core.contracts.StateAndRef
import com.r3corda.core.protocols.ProtocolLogicRefFactory
import com.r3corda.node.services.api.ServiceHubInternal

/**
 * This observes the wallet and schedules and unschedules activities appropriately based on state production and
 * consumption.
 */
class ScheduledActivityObserver(val services: ServiceHubInternal) {
    init {
        // TODO: Need to consider failure scenarios.  This needs to run if the TX is successfully recorded
        services.walletService.updates.subscribe { update ->
            update.consumed.forEach { services.schedulerService.unscheduleStateActivity(it) }
            update.produced.forEach { scheduleStateActivity(it, services.protocolLogicRefFactory) }
        }
    }

    private fun scheduleStateActivity(produced: StateAndRef<ContractState>, protocolLogicRefFactory: ProtocolLogicRefFactory) {
        val producedState = produced.state.data
        if (producedState is SchedulableState) {
            val scheduledAt = sandbox { producedState.nextScheduledActivity(produced.ref, protocolLogicRefFactory)?.scheduledAt } ?: return
            services.schedulerService.scheduleStateActivity(ScheduledStateRef(produced.ref, scheduledAt))
        }
    }

    // TODO: Beware we are calling dynamically loaded contract code inside here.
    private inline fun <T : Any> sandbox(code: () -> T?): T? {
        return code()
    }
}