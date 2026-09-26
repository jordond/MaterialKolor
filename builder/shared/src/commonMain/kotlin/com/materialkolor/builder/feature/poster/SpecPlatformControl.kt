package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.extras_platform_label
import com.materialkolor.builder.generated.resources.extras_platform_phone
import com.materialkolor.builder.generated.resources.extras_platform_watch
import com.materialkolor.builder.generated.resources.extras_spec_2021
import com.materialkolor.builder.generated.resources.extras_spec_2025
import com.materialkolor.builder.generated.resources.extras_spec_2026
import com.materialkolor.builder.generated.resources.extras_spec_2026_cmf
import com.materialkolor.builder.generated.resources.extras_spec_classic_only
import com.materialkolor.builder.generated.resources.extras_spec_cmf_only
import com.materialkolor.builder.generated.resources.extras_spec_comes_back
import com.materialkolor.builder.generated.resources.extras_spec_label
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The spec and the platform.
 *
 * The choice shows all three specs with the one the scheme is really built with chosen, and turns
 * off the specs the style has no form in with a line saying why. A style with a 2025 form offers
 * 2021 and 2025, a 2021 only style offers just 2021, and Cmf offers just 2026. Changing the style
 * never touches the spec the document asked for, so a 2025 asked for under a 2021 only style comes
 * back with the next style that has it, and the control says as much while it waits. The platform
 * only shows where the effective spec has one.
 */
@Composable
internal fun SpecPlatformControl(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val specState = context.capabilities[Control.SpecVersion]
    val platformState = context.capabilities[Control.Platform]
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        if (specState.shown) {
            SpecChoice(context, dispatcher, enabled = specState.usable)
            specState.explanation?.let { reason -> ReasonLine(reason) }
        }
        if (platformState.shown) {
            PlatformChoice(context, dispatcher, enabled = platformState.usable)
            platformState.explanation?.let { reason -> ReasonLine(reason) }
        }
    }
}

/**
 * The spec choice for the document's style, with the specs it has no form in turned off.
 */
@Composable
private fun SpecChoice(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    enabled: Boolean,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val document = context.document
    val offered = EffectiveSpec.offered(document.style)
    val effective = EffectiveSpec.of(document.style, document.spec)
    val label = stringResource(Res.string.extras_spec_label)
    val names = SpecVersion.entries.associateWith { spec -> stringResource(specName(spec)) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        InfoLabel(label = label, topic = InfoTopic.Spec)
        BuilderSegmented(
            options = SpecVersion.entries,
            selected = effective,
            onSelect = { spec ->
                if (spec != effective) {
                    dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetSpec(spec), EditPhase.Discrete))
                }
            },
            label = label,
            enabled = enabled,
            optionEnabled = { spec -> spec in offered },
            optionLabel = { spec -> names.getValue(spec) },
        )
        OffSpecLines(offered = offered, asked2025 = document.spec == SpecVersion.Spec2025)
    }
}

/**
 * Why the specs missing from [offered] are off. Under a 2021 only style it also says, while the
 * document asks for 2025, that 2025 comes back with the next style that has it.
 */
@Composable
private fun OffSpecLines(
    offered: Set<SpecVersion>,
    asked2025: Boolean,
) {
    val classicOnly = offered.singleOrNull() == SpecVersion.Spec2021
    val reason = when {
        classicOnly -> Res.string.extras_spec_classic_only
        SpecVersion.Spec2026 in offered -> Res.string.extras_spec_cmf_only
        else -> Res.string.extras_spec_2026_cmf
    }
    BuilderText(text = stringResource(reason), emphasis = Emphasis.Secondary)
    if (classicOnly && asked2025) {
        BuilderText(text = stringResource(Res.string.extras_spec_comes_back), emphasis = Emphasis.Secondary)
    }
}

/**
 * Phone or Watch, the device the scheme is tuned for.
 */
@Composable
private fun PlatformChoice(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    enabled: Boolean,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val label = stringResource(Res.string.extras_platform_label)
    val names = SchemePlatform.entries.associateWith { platform -> stringResource(platformName(platform)) }
    val platform = context.document.platform
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderText(text = label, style = BuilderTextStyle.SectionLabel)
        BuilderSegmented(
            options = SchemePlatform.entries,
            selected = platform,
            onSelect = { picked ->
                if (picked != platform) {
                    dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetPlatform(picked), EditPhase.Discrete))
                }
            },
            label = label,
            enabled = enabled,
            optionLabel = { option -> names.getValue(option) },
        )
    }
}

/**
 * What [spec] is called, the year it came out.
 */
internal fun specName(spec: SpecVersion): StringResource =
    when (spec) {
        SpecVersion.Spec2021 -> Res.string.extras_spec_2021
        SpecVersion.Spec2025 -> Res.string.extras_spec_2025
        SpecVersion.Spec2026 -> Res.string.extras_spec_2026
    }

/**
 * What [platform] is called.
 */
private fun platformName(platform: SchemePlatform): StringResource =
    when (platform) {
        SchemePlatform.Phone -> Res.string.extras_platform_phone
        SchemePlatform.Watch -> Res.string.extras_platform_watch
    }
