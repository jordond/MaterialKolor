package com.materialkolor.builder.settings.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.collections.immutable.toPersistentList
import materialkolorbuilder.app.generated.resources.Res
import materialkolorbuilder.app.generated.resources.content_based_color_scheme_1
import materialkolorbuilder.app.generated.resources.content_based_color_scheme_2
import materialkolorbuilder.app.generated.resources.content_based_color_scheme_3
import materialkolorbuilder.app.generated.resources.content_based_color_scheme_4
import materialkolorbuilder.app.generated.resources.content_based_color_scheme_5
import org.jetbrains.compose.resources.DrawableResource

@Immutable
sealed class SeedImage {
    abstract val id: String
    abstract val color: Color

    @Immutable
    data class Resource(
        override val id: String,
        val drawableResource: DrawableResource,
        override val color: Color,
    ) : SeedImage() {
        constructor(
            id: Int,
            drawableResource: DrawableResource,
            color: Color,
        ) : this("res-$id", drawableResource, color)
    }

    @Immutable
    data class Custom(val image: ImageBitmap, override val color: Color) : SeedImage() {
        override val id: String = "custom"
    }
}

object ImagePresets {
    val one = Res.drawable.content_based_color_scheme_1 to Color(-9919232)
    val two = Res.drawable.content_based_color_scheme_2 to Color(-3416594)
    val three = Res.drawable.content_based_color_scheme_3 to Color(-1285996)
    val four = Res.drawable.content_based_color_scheme_4 to Color(-7666944)
    val five = Res.drawable.content_based_color_scheme_5 to Color(-11772708)

    val all = listOf(one, two, three, four, five)
        .mapIndexed { index, (res, color) -> SeedImage.Resource(index, res, color) }
        .toPersistentList()
}
