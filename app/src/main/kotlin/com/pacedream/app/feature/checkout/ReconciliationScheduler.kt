package com.pacedream.app.feature.checkout

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin abstraction over [PaymentReconciliationWorker.enqueue] / [PaymentReconciliationWorker.cancel].
 *
 * Exists so [CheckoutViewModel] doesn't have to hold an Android `Context`
 * just to talk to WorkManager — which in turn makes the view-model
 * unit-testable without Robolectric (the only callers of the worker
 * sit behind this seam).
 */
interface ReconciliationScheduler {
    fun enqueue()
    fun cancel()
}

@Singleton
class WorkManagerReconciliationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReconciliationScheduler {
    override fun enqueue() {
        PaymentReconciliationWorker.enqueue(context)
    }

    override fun cancel() {
        PaymentReconciliationWorker.cancel(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ReconciliationSchedulerModule {
    @Binds
    @Singleton
    abstract fun bindReconciliationScheduler(
        impl: WorkManagerReconciliationScheduler,
    ): ReconciliationScheduler
}
