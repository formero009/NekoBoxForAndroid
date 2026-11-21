package io.nekohasekai.sagernet.group

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnMainDispatcher
import io.nekohasekai.sagernet.ui.ThemedActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GroupInterfaceAdapter(val context: ThemedActivity) : GroupManager.Interface {

    private fun canShowUi() = !context.isFinishing && !context.isDestroyed

    override suspend fun confirm(message: String): Boolean = onMainDispatcher {
        if (!canShowUi()) return@onMainDispatcher false
        suspendCancellableCoroutine { continuation ->
            val dialog = MaterialAlertDialogBuilder(context).setTitle(R.string.confirm)
                .setMessage(message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    if (continuation.isActive) continuation.resume(true)
                }
                .setNegativeButton(R.string.no, null)
                .show()
            dialog.setOnDismissListener {
                if (continuation.isActive) continuation.resume(false)
            }
            continuation.invokeOnCancellation { runOnMainDispatcher { dialog.dismiss() } }
        }
    }

    override suspend fun onUpdateSuccess(
        group: ProxyGroup,
        changed: Int,
        added: List<String>,
        updated: Map<String, String>,
        deleted: List<String>,
        duplicate: List<String>,
        byUser: Boolean
    ) = onMainDispatcher {
        if (!canShowUi()) return@onMainDispatcher
        if (changed == 0 && duplicate.isEmpty()) {
            if (byUser) context.snackbar(
                    context.getString(
                            R.string.group_no_difference, group.displayName()
                    )
            ).show()
        } else {
            context.snackbar(context.getString(R.string.group_updated, group.name, changed)).show()
            if (!byUser) return@onMainDispatcher

            var status = ""
            if (added.isNotEmpty()) {
                status += context.getString(
                        R.string.group_added, added.joinToString("\n", postfix = "\n\n")
                )
            }
            if (updated.isNotEmpty()) {
                status += context.getString(R.string.group_changed,
                        updated.map { it }.joinToString("\n", postfix = "\n\n") {
                            if (it.key == it.value) it.key else "${it.key} => ${it.value}"
                        })
            }
            if (deleted.isNotEmpty()) {
                status += context.getString(
                        R.string.group_deleted, deleted.joinToString("\n", postfix = "\n\n")
                )
            }
            if (duplicate.isNotEmpty()) {
                status += context.getString(
                        R.string.group_duplicate, duplicate.joinToString("\n", postfix = "\n\n")
                )
            }

            delay(1000L)
            if (canShowUi()) {
                MaterialAlertDialogBuilder(context).setTitle(
                        context.getString(
                                R.string.group_diff, group.displayName()
                        )
                ).setMessage(status.trim()).setPositiveButton(android.R.string.ok, null).show()
            }

        }

    }

    override suspend fun onUpdateFailure(group: ProxyGroup, message: String) {
        onMainDispatcher {
            if (canShowUi()) context.snackbar(message).show()
        }
    }

    override suspend fun alert(message: String) {
        return suspendCoroutine {
            runOnMainDispatcher {
                MaterialAlertDialogBuilder(context).setTitle(R.string.ooc_warning)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> it.resume(Unit) }
                    .setOnCancelListener { _ -> it.resume(Unit) }
                    .show()
            }
        }
    }

}
