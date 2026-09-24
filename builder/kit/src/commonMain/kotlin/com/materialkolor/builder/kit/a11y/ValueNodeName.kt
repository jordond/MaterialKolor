package com.materialkolor.builder.kit.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.stateDescription
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.roleLessName

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
 * Names a node that plays no role but holds a value, such as the handle between the two copies of
 * a split preview.
 *
 * Where [folds] holds, on the web, the name and the value go in as one text, "Split, 50% Light".
 * Elsewhere [name] is the content description. The value is the state description everywhere, for
 * when the web mirror carries it across.
 *
 * Call it in a semantics block, so a value that moves with a drag is written there and nothing
 * recomposes.
 *
 * @param[name] What the node is, such as "Split".
 * @param[value] What it holds now, such as "50% Light".
 * @param[folds] [foldsValueIntoName], read in composition.
 */
public fun SemanticsPropertyReceiver.valueNodeName(
    name: String,
    value: String,
    folds: Boolean,
) {
    val named = if (folds) listOf(name, value).filter { part -> part.isNotBlank() }.joinToString(", ") else name
    roleLessName(named, asText = folds)
    stateDescription = value
}
