package com.materialkolor.builder.kit.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.stateDescription
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.role_progress_bar
import com.materialkolor.builder.kit.generated.resources.role_slider
import org.jetbrains.compose.resources.stringResource

/**
 * Whether a node that plays no role carries its value in its name here. Read it in composition and
 * hand it to [valueNodeName].
 *
 * It holds on the web (D37). The web mirror in CMP 1.12.1 drops a state description, and screen
 * readers drop an `aria-label` on an element with no role, while both keep text.
 */
public val foldsValueIntoName: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalFoldsStateIntoName.current

/**
 * The kit's word for a slider as it reads after a name, "slider", in the current locale. Read it in
 * composition and hand it to [valueNodeName] for a node that works as a slider, so it reads the way
 * the kit's own sliders do on the web.
 */
public val sliderRoleWord: String
    @Composable
    get() = stringResource(Res.string.role_slider)

/**
 * The kit's word for a progress bar as it reads after a name, "progress bar", in the current locale.
 * Read it in composition and hand it to [valueNodeName] for a node that shows how far along some
 * work is, so it reads the way the kit's own progress bars do on the web, "Upload, progress bar,
 * 40%".
 */
public val progressRoleWord: String
    @Composable
    get() = stringResource(Res.string.role_progress_bar)

/**
 * Names a node that plays no role but holds a value, such as the handle between the two copies of
 * a split preview.
 *
 * Where [folds] holds, on the web, the name, the role word and the value go in as one text, "Split,
 * slider, 50% Light". Elsewhere [name] is the content description. The value is the state
 * description everywhere, for when the web mirror carries it across.
 *
 * Call it in a semantics block, so a value that moves with a drag is written there and nothing
 * recomposes.
 *
 * @param[name] What the node is, such as "Split".
 * @param[value] What it holds now, such as "50% Light".
 * @param[folds] [foldsValueIntoName], read in composition.
 * @param[roleWord] The word for what the node works as, such as [sliderRoleWord] or
 * [progressRoleWord], read on the web
 * between the name and the value. Left null, the node names no role.
 */
public fun SemanticsPropertyReceiver.valueNodeName(
    name: String,
    value: String,
    folds: Boolean,
    roleWord: String? = null,
) {
    val named = if (folds) {
        listOfNotNull(name, roleWord, value).filter { part -> part.isNotBlank() }.joinToString(", ")
    } else {
        name
    }
    roleLessName(named, asText = folds)
    stateDescription = value
}
