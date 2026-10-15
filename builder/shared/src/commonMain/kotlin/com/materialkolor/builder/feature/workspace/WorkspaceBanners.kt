package com.materialkolor.builder.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.feature.projects.ConflictBanner
import com.materialkolor.builder.feature.projects.NewerDataBanner
import com.materialkolor.builder.feature.projects.ProjectBanner
import com.materialkolor.builder.feature.projects.ProjectsAction
import com.materialkolor.builder.feature.projects.ProjectsModel
import com.materialkolor.builder.feature.projects.StorageUnavailableBanner
import com.materialkolor.builder.feature.share.SharedLinkBanner
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.banners_dismiss
import com.materialkolor.builder.generated.resources.banners_get_link
import com.materialkolor.builder.generated.resources.banners_invalid_link
import com.materialkolor.builder.generated.resources.banners_invalid_link_defaults
import com.materialkolor.builder.generated.resources.banners_newer_version
import com.materialkolor.builder.generated.resources.banners_reload
import com.materialkolor.builder.generated.resources.banners_start_from_defaults
import com.materialkolor.builder.generated.resources.banners_storage_full
import com.materialkolor.builder.generated.resources.banners_storage_full_projects
import com.materialkolor.builder.generated.resources.banners_unknown_path
import com.materialkolor.builder.generated.resources.projects_conflict
import com.materialkolor.builder.generated.resources.projects_newer_data
import com.materialkolor.builder.generated.resources.projects_storage_unavailable
import com.materialkolor.builder.generated.resources.share_transient
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.MediumBreakpoint
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The one stack of banners below the top bar, wired to the models that raise them.
 *
 * The app model says why the address did not open what it asked for and how storage is doing, and
 * the projects model says whether another tab clashed, whether the theme is saved yet and whether a
 * newer build saved data here. Opening Projects or a link goes through [dispatcher].
 *
 * @param[state] The workspace, for the panel that is open.
 */
@Composable
internal fun WorkspaceBanners(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    app: AppModel = metroViewModel(),
    projects: ProjectsModel = metroViewModel(),
) {
    val appState by app.collectAsState()
    val projectsState by projects.collectAsState()
    WorkspaceBanners(
        app = appState,
        projects = projectsState,
        drawerOpen = state.panel == Panel.Projects,
        onAction = { action ->
            when (action) {
                is BannerAction.ResolveConflict -> {
                    projects.handle(ProjectsAction.ResolveConflict(action.keepMine))
                }
                BannerAction.StartFromDefaults -> {
                    app.dismissBootNotice()
                    projects.handle(ProjectsAction.New(copyCurrent = false))
                }
                BannerAction.DismissBootNotice -> {
                    app.dismissBootNotice()
                }
                BannerAction.ReloadLink -> {
                    app.reloadLink()
                }
                BannerAction.OpenProjects -> {
                    dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Projects))
                }
                BannerAction.GetLink -> {
                    dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Share))
                }
                BannerAction.CloseStorageUnavailable -> {
                    app.dismissStorageUnavailable()
                }
                BannerAction.SaveTheme -> {
                    projects.handle(ProjectsAction.SaveShared)
                }
                BannerAction.ReloadHome -> {
                    app.reloadHome()
                }
                BannerAction.DismissNewerData -> {
                    app.dismissNewerData()
                }
            }
        },
        modifier = modifier,
    )
}

/**
 * The banners [app] and [projects] raise, drawn as one stack, with every button handing what it
 * asks for to [onAction].
 *
 * @param[drawerOpen] Whether the projects drawer is open, which says itself that storage is missing.
 */
@Composable
internal fun WorkspaceBanners(
    app: AppModel.State,
    projects: ProjectsModel.State,
    onAction: (BannerAction) -> Unit,
    modifier: Modifier = Modifier,
    drawerOpen: Boolean = false,
) {
    val banners = remember(app, projects, drawerOpen) { workspaceBanners(app, projects, drawerOpen) }
    BannerStack(banners = banners, canReload = app.canReload, onAction = onAction, modifier = modifier)
}

/**
 * One banner in the stack, in the order they stack.
 */
internal enum class WorkspaceBanner {
    /**
     * Another tab saved the open project while this one was editing it.
     */
    Conflict,

    /**
     * A link could not be read, so boot opened the last theme instead.
     */
    InvalidLink,

    /**
     * A link could not be read, so boot opened the defaults instead.
     */
    InvalidLinkDefaults,

    /**
     * The address had no page, so boot opened the builder at home.
     */
    UnknownPath,

    /**
     * A link was written by a newer builder.
     */
    NewerVersion,

    /**
     * Storage stayed full after the repository made room, so the last save did not land.
     */
    StorageFull,

    /**
     * Nothing saved here outlives the session.
     */
    StorageUnavailable,

    /**
     * The open theme is not saved to the drawer yet.
     */
    UnsavedTheme,

    /**
     * A newer build saved data this one leaves alone until a reload.
     */
    NewerData,
}

/**
 * What a banner in the stack can ask for.
 */
internal sealed interface BannerAction {
    /**
     * Settle a clash with another tab, keeping this tab's document or taking the other's.
     */
    data class ResolveConflict(
        val keepMine: Boolean,
    ) : BannerAction

    /**
     * Start a project from the defaults in place of the theme boot opened.
     */
    data object StartFromDefaults : BannerAction

    /**
     * Put the boot notice away.
     */
    data object DismissBootNotice : BannerAction

    /**
     * Load the link the page opened on again, for a newer build to read.
     */
    data object ReloadLink : BannerAction

    /**
     * Open the projects drawer.
     */
    data object OpenProjects : BannerAction

    /**
     * Open the share dialog for a link to the theme.
     */
    data object GetLink : BannerAction

    /**
     * Close the banner about a browser that keeps nothing.
     */
    data object CloseStorageUnavailable : BannerAction

    /**
     * Save the theme that is not saved yet to the drawer.
     */
    data object SaveTheme : BannerAction

    /**
     * Load the builder again at `/`, for a newer build to pick up its data.
     */
    data object ReloadHome : BannerAction

    /**
     * Put the banner about a newer build's data away, where a reload does nothing.
     */
    data object DismissNewerData : BannerAction
}

/**
 * The banners [app] and [projects] raise, in the order they stack. A clash with another tab comes
 * first, then the boot notice, then storage, then a theme that is not saved yet, then data a newer
 * build saved. Without storage there is nowhere to save the theme, and with storage full a save would
 * fail the same way, so that banner stays away while the storage full one offers a way out.
 *
 * @param[drawerOpen] Whether the projects drawer is open. It says itself that storage is missing, so
 * the stack leaves that banner to it.
 */
internal fun workspaceBanners(
    app: AppModel.State,
    projects: ProjectsModel.State,
    drawerOpen: Boolean = false,
): List<WorkspaceBanner> =
    buildList {
        if (projects.conflict) add(WorkspaceBanner.Conflict)
        bootBanner(app)?.let(::add)
        if (app.storageFull) add(WorkspaceBanner.StorageFull)
        if (app.storageUnavailable && !app.storageUnavailableDismissed && !drawerOpen) {
            add(WorkspaceBanner.StorageUnavailable)
        }
        if (projects.transient && projects.storageAvailable && !app.storageFull) add(WorkspaceBanner.UnsavedTheme)
        if (projects.newerData && !app.newerDataDismissed) add(WorkspaceBanner.NewerData)
    }

private fun bootBanner(app: AppModel.State): WorkspaceBanner? {
    val invalidLink = if (app.openedDefaults) WorkspaceBanner.InvalidLinkDefaults else WorkspaceBanner.InvalidLink
    return when (app.bootNotice) {
        null -> null
        BootNotice.InvalidLink -> invalidLink
        BootNotice.UnknownPath -> WorkspaceBanner.UnknownPath
        BootNotice.NewerVersion -> WorkspaceBanner.NewerVersion
    }
}

/**
 * Draws [banners] from the top down, below the top bar. A banner that shows up leaves focus where
 * it is and is read out once through [LocalAnnouncer], and a screen reader reads the stack as one
 * group, in order.
 *
 * @param[canReload] Whether Reload does anything here. Without it the banners that would ask for
 * one offer Dismiss only.
 */
@Composable
internal fun BannerStack(
    banners: List<WorkspaceBanner>,
    canReload: Boolean,
    onAction: (BannerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (banners.isEmpty()) return
    val spacing = LocalBuilderTokens.current.spacing
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Two section gaps clear the top bar.
                .padding(top = spacing.section + spacing.section, start = spacing.medium, end = spacing.medium)
                .widthIn(max = MediumBreakpoint)
                .fillMaxWidth()
                .semantics { isTraversalGroup = true },
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            banners.forEach { banner ->
                key(banner) {
                    AnnounceOnce(stringResource(messageOf(banner)))
                    Banner(banner, canReload, onAction)
                }
            }
        }
    }
}

/**
 * Reads [message] out once through [LocalAnnouncer] as its banner shows up. A recomposition or a
 * move in the stack says nothing more, and a banner that goes and comes back is read out again. An
 * empty message is one the resources have not loaded yet.
 */
@Composable
private fun AnnounceOnce(message: String) {
    val announcer = LocalAnnouncer.current
    LaunchedEffect(message) {
        if (message.isNotEmpty()) announcer.announce(message)
    }
}

/**
 * What [banner] says, the text it is read out with.
 */
private fun messageOf(banner: WorkspaceBanner): StringResource =
    when (banner) {
        WorkspaceBanner.Conflict -> Res.string.projects_conflict
        WorkspaceBanner.InvalidLink -> Res.string.banners_invalid_link
        WorkspaceBanner.InvalidLinkDefaults -> Res.string.banners_invalid_link_defaults
        WorkspaceBanner.UnknownPath -> Res.string.banners_unknown_path
        WorkspaceBanner.NewerVersion -> Res.string.banners_newer_version
        WorkspaceBanner.StorageFull -> Res.string.banners_storage_full
        WorkspaceBanner.StorageUnavailable -> Res.string.projects_storage_unavailable
        WorkspaceBanner.UnsavedTheme -> Res.string.share_transient
        WorkspaceBanner.NewerData -> Res.string.projects_newer_data
    }

@Composable
private fun Banner(
    banner: WorkspaceBanner,
    canReload: Boolean,
    onAction: (BannerAction) -> Unit,
) {
    when (banner) {
        WorkspaceBanner.Conflict -> {
            ConflictBanner(onResolve = { keepMine -> onAction(BannerAction.ResolveConflict(keepMine)) })
        }
        WorkspaceBanner.InvalidLink -> {
            BootNoticeBanner(messageOf(banner), onAction) {
                BuilderButton(
                    onClick = { onAction(BannerAction.StartFromDefaults) },
                    label = stringResource(Res.string.banners_start_from_defaults),
                )
            }
        }
        WorkspaceBanner.InvalidLinkDefaults -> {
            BootNoticeBanner(messageOf(banner), onAction)
        }
        WorkspaceBanner.UnknownPath -> {
            BootNoticeBanner(messageOf(banner), onAction)
        }
        WorkspaceBanner.NewerVersion -> {
            BootNoticeBanner(messageOf(banner), onAction, icon = IconId.Info) {
                if (canReload) {
                    BuilderButton(
                        onClick = { onAction(BannerAction.ReloadLink) },
                        label = stringResource(Res.string.banners_reload),
                        emphasis = Emphasis.Primary,
                    )
                }
            }
        }
        WorkspaceBanner.StorageFull -> {
            StorageFullBanner(onAction)
        }
        WorkspaceBanner.StorageUnavailable -> {
            StorageUnavailableBanner(
                onGetLink = { onAction(BannerAction.GetLink) },
                onClose = { onAction(BannerAction.CloseStorageUnavailable) },
            )
        }
        WorkspaceBanner.UnsavedTheme -> {
            SharedLinkBanner(onSave = { onAction(BannerAction.SaveTheme) })
        }
        WorkspaceBanner.NewerData -> {
            if (canReload) {
                NewerDataBanner(onReload = { onAction(BannerAction.ReloadHome) })
            } else {
                NewerDataBanner(onDismiss = { onAction(BannerAction.DismissNewerData) })
            }
        }
    }
}

/**
 * Why the address did not open what it asked for, with [actions] ahead of Dismiss. Boot has opened
 * the last theme or the defaults by now, so the message says which.
 */
@Composable
private fun BootNoticeBanner(
    message: StringResource,
    onAction: (BannerAction) -> Unit,
    icon: IconId = IconId.Warning,
    actions: @Composable () -> Unit = {},
) {
    ProjectBanner(icon = icon, message = stringResource(message)) {
        actions()
        BuilderButton(
            onClick = { onAction(BannerAction.DismissBootNotice) },
            label = stringResource(Res.string.banners_dismiss),
            emphasis = Emphasis.Subtle,
        )
    }
}

/**
 * Storage stayed full after the old histories were cleared, so the last save did not land. Projects
 * opens the drawer to delete one, and Get a link keeps this theme without storage.
 */
@Composable
private fun StorageFullBanner(onAction: (BannerAction) -> Unit) {
    ProjectBanner(icon = IconId.Error, message = stringResource(Res.string.banners_storage_full)) {
        BuilderButton(
            onClick = { onAction(BannerAction.OpenProjects) },
            label = stringResource(Res.string.banners_storage_full_projects),
            icon = IconId.Folder,
        )
        BuilderButton(
            onClick = { onAction(BannerAction.GetLink) },
            label = stringResource(Res.string.banners_get_link),
            emphasis = Emphasis.Primary,
            icon = IconId.Share,
        )
    }
}
