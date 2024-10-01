package com.materialkolor.builder.ui.home.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.materialkolor.builder.export.model.ExportFile
import com.materialkolor.builder.ui.components.CopyIcon
import com.materialkolor.builder.ui.components.code.CodeTextView
import com.materialkolor.builder.ui.theme.JetBrainsMono
import com.materialkolor.builder.ui.theme.LocalThemeIsDark
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.collections.immutable.PersistentList

@Composable
fun FileListContainer(
    selected: ExportFile,
    files: PersistentList<ExportFile>,
    onSelected: (ExportFile) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(selected, files) {
        if (selected.name !in files.map { it.name }) {
            Logger.d { "Selected file ${selected.name} not in list, selecting first file" }
            onSelected(files.first())
        }
    }

    OutlinedCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            files.forEach { file ->
                Tab(
                    file = file,
                    isSelected = selected.name == file.name,
                    onClick = { onSelected(file) },
                )
            }
        }

        Box(modifier = Modifier) {
            val isDark by LocalThemeIsDark.current
            val highlights by remember(files, selected) {
                mutableStateOf(
                    Highlights
                        .Builder()
                        .code(selected.content)
                        .language(selected.language)
                        .theme(SyntaxThemes.atom(isDark))
                        .build(),
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.padding(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                ) {

                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            CodeTextView(
                                highlights = highlights,
                                style = LocalTextStyle.current.copy(
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Light,
                                ),
                                modifier = Modifier.fillMaxSize()
                            )

                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }

                    Box(
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text("Copy whole file to clipboard") } },
                            state = rememberTooltipState(),
                        ) {
                            FilledTonalIconButton(
                                onClick = onClick,
                            ) {
                                CopyIcon(visble = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tab(
    file: ExportFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant

    val shape = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 8.dp,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(backgroundColor, shape)
            .clip(shape)
            .clickable(enabled = !isSelected, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .widthIn(max = 200.dp),
    ) {
        Text(
            text = file.name,
            color = contentColorFor(backgroundColor).copy(if (isSelected) 1f else 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = LocalTextStyle.current.copy(
                fontFamily = JetBrainsMono,
                fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Thin,
            ),
        )
    }
}
