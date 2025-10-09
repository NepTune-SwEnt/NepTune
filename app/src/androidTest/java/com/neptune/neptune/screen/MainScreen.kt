package com.neptune.neptune.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.neptune.neptune.resources.C
import io.github.kakaocup.compose.node.element.ComposeScreen

class MainScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(C.Tag.main_screen_container) }) {}
