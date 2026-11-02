package com.arjunvision.arjunfitai.ui.workout

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arjunvision.arjunfitai.data.workout.Exercise
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.WorkoutRepository
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfTextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object WorkoutRoutes {
    const val HUB = "workout"
    const val LIBRARY = "workout/library"
    const val PLANS = "workout/plans"
    const val LIVE = "workout/live"
    const val HISTORY = "workout/history"
    const val PRS = "workout/prs"
    fun exercise(id: String) = "workout/exercise/${Uri.encode(id)}"
    fun plan(id: Long) = "workout/plan/$id"
    fun pickFor(planId: Long) = "$LIBRARY?plan=$planId"
}

/** All Workout screens (Phase 5). [library] loads in the background on first use. */
fun NavGraphBuilder.workoutGraph(
    nav: NavController,
    padding: PaddingValues,
    repo: WorkoutRepository,
    timer: RestTimer,
    library: State<List<Exercise>>,
) {
    composable(WorkoutRoutes.HUB) {
        val plans by repo.plans.collectAsStateWithLifecycle(emptyList())
        val items by repo.planItems.collectAsStateWithLifecycle(emptyList())
        val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
        val active by repo.activeSession.collectAsStateWithLifecycle(null)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var newPlan by remember { mutableStateOf(false) }
        HubScreen(
            plans, items, sessions, active, library.value.associateBy { it.id },
            HubActions(
                onStart = { plan -> startOrResume(scope, repo, active != null, plan, nav) { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } },
                onResume = { nav.navigate(WorkoutRoutes.LIVE) },
                onLibrary = { nav.navigate(WorkoutRoutes.LIBRARY) },
                onPlans = { nav.navigate(WorkoutRoutes.PLANS) },
                onHistory = { nav.navigate(WorkoutRoutes.HISTORY) },
                onPrs = { nav.navigate(WorkoutRoutes.PRS) },
                onNewPlan = { newPlan = true },
            ),
            padding,
        )
        if (newPlan) NewPlanDialog(onDismiss = { newPlan = false }) { name ->
            newPlan = false
            scope.launch { nav.navigate(WorkoutRoutes.plan(repo.createPlan(name))) }
        }
    }

    composable(
        "${WorkoutRoutes.LIBRARY}?plan={plan}",
        arguments = listOf(navArgument("plan") { type = NavType.LongType; defaultValue = -1L }),
    ) { entry ->
        val planId = entry.arguments?.getLong("plan")?.takeIf { it > 0 }
        val plans by repo.plans.collectAsStateWithLifecycle(emptyList())
        val pickingFor = plans.firstOrNull { it.id == planId }
        AddFlow(repo, plans, pickingFor) { requestAdd ->
            LibraryScreen(
                library.value, pickingFor?.name, padding,
                onBack = { nav.popBackStack() },
                onOpen = { nav.navigate(WorkoutRoutes.exercise(it.id)) },
                onAdd = requestAdd,
            )
        }
    }

    composable("workout/exercise/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("id")
        val exercise = library.value.firstOrNull { it.id == id }
        val plans by repo.plans.collectAsStateWithLifecycle(emptyList())
        if (exercise != null) {
            AddFlow(repo, plans, pickingFor = null) { requestAdd ->
                ExerciseDetailScreen(exercise, padding, onBack = { nav.popBackStack() }, onAdd = { requestAdd(exercise) })
            }
        }
    }

    composable(WorkoutRoutes.PLANS) {
        val plans by repo.plans.collectAsStateWithLifecycle(emptyList())
        val items by repo.planItems.collectAsStateWithLifecycle(emptyList())
        val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
        val active by repo.activeSession.collectAsStateWithLifecycle(null)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var newPlan by remember { mutableStateOf(false) }
        PlansScreen(
            plans, items, sessions, padding,
            onBack = { nav.popBackStack() },
            onOpen = { nav.navigate(WorkoutRoutes.plan(it.id)) },
            onStart = { plan -> startOrResume(scope, repo, active != null, plan, nav) { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } },
            onNewPlan = { newPlan = true },
        )
        if (newPlan) NewPlanDialog(onDismiss = { newPlan = false }) { name ->
            newPlan = false
            scope.launch { nav.navigate(WorkoutRoutes.plan(repo.createPlan(name))) }
        }
    }

    composable("workout/plan/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
        val id = entry.arguments?.getLong("id")
        val plans by repo.plans.collectAsStateWithLifecycle(null)
        val items by repo.planItems.collectAsStateWithLifecycle(emptyList())
        val active by repo.activeSession.collectAsStateWithLifecycle(null)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val plan = plans?.firstOrNull { it.id == id }
        if (plan != null) {
            PlanEditScreen(
                plan, items.filter { it.planId == plan.id }, library.value.associateBy { it.id }, padding,
                PlanEditActions(
                    onBack = { nav.popBackStack() },
                    onStart = { startOrResume(scope, repo, active != null, plan, nav) { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } },
                    onAddExercise = { nav.navigate(WorkoutRoutes.pickFor(plan.id)) },
                    onUpdate = { scope.launch { repo.updateItem(it) } },
                    onMove = { item, by -> scope.launch { repo.moveItem(item, by) } },
                    onRemove = { scope.launch { repo.removeItem(it) } },
                    onRename = { scope.launch { repo.renamePlan(plan, it) } },
                    onDelete = { scope.launch { repo.deletePlan(plan); nav.popBackStack() } },
                ),
            )
        } else if (plans != null) {
            LaunchedEffect(Unit) { nav.popBackStack() }
        }
    }

    composable(WorkoutRoutes.LIVE) {
        val active by repo.activeSession.collectAsStateWithLifecycle(null)
        val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
        val sets by repo.sets.collectAsStateWithLifecycle(emptyList())
        val items by repo.planItems.collectAsStateWithLifecycle(emptyList())
        val session = active ?: sessions.firstOrNull { it.end == null }
        if (session != null) {
            LiveWorkoutScreen(
                session = session,
                sessionSets = sets.filter { it.sessionId == session.id },
                planItems = items.filter { it.planId == session.planId },
                allSets = sets,
                allSessions = sessions,
                library = library.value.associateBy { it.id },
                timer = timer,
                contentPadding = padding,
                actions = LiveActions(
                    onBack = { nav.popBackStack() },
                    onUpdate = repo::updateSet,
                    onAddSet = { repo.addSet(it) },
                    onFinish = { anyDone ->
                        repo.finish(session, anyDone)
                        nav.popBackStack()
                        if (anyDone) nav.navigate(WorkoutRoutes.HISTORY)
                    },
                    onHowTo = { nav.navigate(WorkoutRoutes.exercise(it.id)) },
                ),
            )
        }
    }

    composable(WorkoutRoutes.HISTORY) {
        val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
        val sets by repo.sets.collectAsStateWithLifecycle(emptyList())
        val scope = rememberCoroutineScope()
        HistoryScreen(sessions, sets, padding, onBack = { nav.popBackStack() }, onDelete = { scope.launch { repo.deleteSession(it) } })
    }

    composable(WorkoutRoutes.PRS) {
        val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
        val sets by repo.sets.collectAsStateWithLifecycle(emptyList())
        PrsScreen(sessions, sets, padding, onBack = { nav.popBackStack() })
    }
}

/** One workout at a time: an open workout is resumed instead of starting a second. */
private fun startOrResume(scope: CoroutineScope, repo: WorkoutRepository, hasActive: Boolean, plan: PlanEntity, nav: NavController, toast: (String) -> Unit) {
    if (hasActive) {
        toast("A workout is already in progress — resuming it")
        nav.navigate(WorkoutRoutes.LIVE)
        return
    }
    scope.launch {
        repo.startSession(plan)
        nav.navigate(WorkoutRoutes.LIVE)
    }
}

/** Wraps a screen with the "add exercise to which plan?" flow; [pickingFor] skips the question. */
@Composable
private fun AddFlow(repo: WorkoutRepository, plans: List<PlanEntity>, pickingFor: PlanEntity?, content: @Composable (requestAdd: (Exercise) -> Unit) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var asking by remember { mutableStateOf<Exercise?>(null) }
    fun added(e: Exercise, planName: String) = Toast.makeText(context, "Added ${e.name} to $planName", Toast.LENGTH_SHORT).show()
    content { e ->
        if (pickingFor != null) scope.launch { repo.addToPlan(pickingFor.id, e); added(e, pickingFor.name) } else asking = e
    }
    asking?.let { e ->
        AddToPlanDialog(
            exercise = e, plans = plans, onDismiss = { asking = null },
            onPick = { p -> asking = null; scope.launch { repo.addToPlan(p.id, e); added(e, p.name) } },
            onNewPlan = { name -> asking = null; scope.launch { repo.addToPlan(repo.createPlan(name), e); added(e, name) } },
        )
    }
}

@Composable
private fun NewPlanDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AfDialog("New plan", onDismiss, "Create" to { onCreate(name.trim()) }, confirmEnabled = name.isNotBlank()) {
        AfTextField(name, { name = it.take(40) }, "Plan name", KeyboardOptions(capitalization = KeyboardCapitalization.Words))
    }
}
