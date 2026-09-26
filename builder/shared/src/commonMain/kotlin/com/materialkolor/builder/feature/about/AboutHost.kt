package com.materialkolor.builder.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.BuildKonfig
import com.materialkolor.builder.domain.link.shareLink
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.about_builder_version
import com.materialkolor.builder.generated.resources.about_close
import com.materialkolor.builder.generated.resources.about_copy_details
import com.materialkolor.builder.generated.resources.about_credits
import com.materialkolor.builder.generated.resources.about_credits_title
import com.materialkolor.builder.generated.resources.about_details_label
import com.materialkolor.builder.generated.resources.about_font_bricolage
import com.materialkolor.builder.generated.resources.about_font_jetbrains_mono
import com.materialkolor.builder.generated.resources.about_font_license
import com.materialkolor.builder.generated.resources.about_font_license_failed
import com.materialkolor.builder.generated.resources.about_github
import com.materialkolor.builder.generated.resources.about_libraries
import com.materialkolor.builder.generated.resources.about_material_kolor_version
import com.materialkolor.builder.generated.resources.about_motion
import com.materialkolor.builder.generated.resources.about_motion_full
import com.materialkolor.builder.generated.resources.about_motion_reduce
import com.materialkolor.builder.generated.resources.about_motion_system
import com.materialkolor.builder.generated.resources.about_privacy
import com.materialkolor.builder.generated.resources.about_privacy_title
import com.materialkolor.builder.generated.resources.about_report
import com.materialkolor.builder.generated.resources.about_title
import com.materialkolor.builder.generated.resources.about_what
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderDisclosure
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.ShippedFont
import com.materialkolor.builder.kit.token.readFontLicense
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.cancellation.CancellationException

/**
 * About, open while `state.panel` is [Panel.About].
 *
 * It names both versions and what the builder is for, holds the motion override, says what stays
 * private, credits what the builder is made from, and ends with GitHub, Report a problem and Copy
 * details. A report never carries the project's name, since issues are public. When the browser
 * refuses Copy details, the workspace opens the details to copy by hand.
 *
 * @param[returnFocusTo] The overflow button that opened it, which gets focus back once it closes.
 */
@Composable
internal fun AboutHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
    model: AboutModel = metroViewModel(),
) {
    val layout = LocalLayout.current
    val spacing = LocalBuilderTokens.current.spacing
    // Taken out here, since a dialog on the desktop provides its own and would skip a caller's.
    val uriHandler = LocalUriHandler.current
    val close = { dispatcher.dispatch(WorkspaceAction.ClosePanel) }
    BuilderDialog(
        visible = state.panel == Panel.About,
        onDismissRequest = close,
        title = stringResource(Res.string.about_title),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        actions = {
            BuilderButton(onClick = close, label = stringResource(Res.string.about_close), emphasis = Emphasis.Primary)
        },
    ) {
        // The dialog's body only composes while it is open, so an edit never works out a link here.
        val details = rememberReportDetails(state.document, model.browser, model.siteOrigin)
        // The intro, the privacy note, the library lines and an opened license hold no control, so
        // the list keeps its own Tab stop for the keyboard to scroll them on the web, as Help does.
        BuilderScrollArea(Modifier.heightIn(max = layout.heightDp * ABOUT_HEIGHT_FRACTION)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
                AboutIntro()
                MotionChoice(state.preferences.motion, dispatcher)
                AboutSection(stringResource(Res.string.about_privacy_title)) {
                    BuilderText(text = stringResource(Res.string.about_privacy), emphasis = Emphasis.Secondary)
                }
                Credits()
                AboutActions(details, uriHandler, dispatcher)
            }
        }
    }
}

/**
 * The report details for [document] in [browser], its link on [origin] worked out once per document.
 */
@Composable
private fun rememberReportDetails(
    document: ThemeDocument,
    browser: String,
    origin: String,
): ReportDetails =
    remember(document, browser, origin) {
        ReportDetails(
            builderVersion = BuildKonfig.BUILDER_VERSION,
            materialKolorVersion = BuildKonfig.MATERIAL_KOLOR_VERSION,
            browser = browser,
            themeLink = shareLink(document, projectName = "", origin = origin),
        )
    }

/**
 * Both versions and a line on what the builder does.
 */
@Composable
private fun AboutIntro() {
    val spacing = LocalBuilderTokens.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Column {
            BuilderText(
                text = stringResource(Res.string.about_builder_version, BuildKonfig.BUILDER_VERSION),
                style = BuilderTextStyle.Label,
            )
            BuilderText(
                text = stringResource(Res.string.about_material_kolor_version, BuildKonfig.MATERIAL_KOLOR_VERSION),
                style = BuilderTextStyle.Label,
            )
        }
        BuilderText(text = stringResource(Res.string.about_what))
    }
}

/**
 * A heading and what it heads.
 */
@Composable
private fun AboutSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderText(text = title, modifier = Modifier.semantics { heading() }, style = BuilderTextStyle.SectionLabel)
        content()
    }
}

/**
 * The motion override. The arrows only move the focus, so the chrome changes how it moves on Enter
 * or Space rather than on every step past an option.
 */
@Composable
private fun MotionChoice(
    motion: MotionOverride,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val title = stringResource(Res.string.about_motion)
    val labels = MotionOverride.entries.associateWith { option -> stringResource(motionLabel(option)) }
    AboutSection(title) {
        BuilderSegmented(
            options = MotionOverride.entries,
            selected = motion,
            onSelect = { option -> dispatcher.dispatch(WorkspaceAction.SetMotionOverride(option)) },
            label = title,
            selectOnFocus = false,
            optionLabel = { option -> labels.getValue(option) },
        )
    }
}

private fun motionLabel(motion: MotionOverride): StringResource =
    when (motion) {
        MotionOverride.System -> Res.string.about_motion_system
        MotionOverride.Reduce -> Res.string.about_motion_reduce
        MotionOverride.Full -> Res.string.about_motion_full
    }

/**
 * Who made the builder, the fonts with their full licenses, and the libraries and icons it ships.
 */
@Composable
private fun Credits() {
    val spacing = LocalBuilderTokens.current.spacing
    AboutSection(stringResource(Res.string.about_credits_title)) {
        BuilderText(text = stringResource(Res.string.about_credits), emphasis = Emphasis.Secondary)
        ShippedFont.entries.forEach { font -> FontLicense(font) }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            stringArrayResource(Res.array.about_libraries).forEach { line ->
                BuilderText(text = line, emphasis = Emphasis.Secondary)
            }
        }
    }
}

/**
 * [font] under its license, the full text read only once it is opened. A text that cannot be read,
 * such as a fetch that fails on the web, leaves a short line with the license's name in its place.
 *
 * @param[read] Reads the full license of a font.
 */
@Composable
internal fun FontLicense(
    font: ShippedFont,
    read: suspend (ShippedFont) -> String = ::readFontLicense,
) {
    var open by rememberSaveable(font) { mutableStateOf(false) }
    val license = stringResource(Res.string.about_font_license)
    BuilderDisclosure(
        expanded = open,
        onExpandedChange = { expanded -> open = expanded },
        title = stringResource(fontName(font)),
        summary = license,
    ) {
        // Null once the read failed. An error thrown in here would stop the whole UI on wasm.
        val text by produceState<String?>("", font) {
            value = runCatching { read(font) }
                .onFailure { error -> if (error is CancellationException) throw error }
                .getOrNull()
        }
        val loaded = text
        if (loaded == null) {
            BuilderText(
                text = stringResource(Res.string.about_font_license_failed, license),
                emphasis = Emphasis.Secondary,
            )
        } else {
            BuilderText(text = loaded, style = BuilderTextStyle.Code)
        }
    }
}

private fun fontName(font: ShippedFont): StringResource =
    when (font) {
        ShippedFont.BricolageGrotesque -> Res.string.about_font_bricolage
        ShippedFont.JetBrainsMono -> Res.string.about_font_jetbrains_mono
    }

/**
 * GitHub, Report a problem and Copy details. The copy writes the clipboard inside the click, and
 * the manual copy dialog a refused copy opens hands focus back to it.
 */
@Composable
private fun AboutActions(
    details: ReportDetails,
    uriHandler: UriHandler,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val copyButton = remember { FocusRequester() }
    val label = stringResource(Res.string.about_details_label)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        BuilderButton(
            onClick = { uriHandler.openUri(GITHUB_URL) },
            label = stringResource(Res.string.about_github),
            icon = IconId.ExternalLink,
        )
        BuilderButton(
            onClick = { uriHandler.openUri(reportUrl(ISSUES_URL, details)) },
            label = stringResource(Res.string.about_report),
            icon = IconId.ExternalLink,
        )
        BuilderButton(
            onClick = {
                dispatcher.dispatch(WorkspaceAction.CopyText(details.text(), label, returnFocusTo = copyButton))
            },
            label = stringResource(Res.string.about_copy_details),
            modifier = Modifier.focusRequester(copyButton),
            icon = IconId.Copy,
        )
    }
}
