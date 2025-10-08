package com.android.sample.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.android.sample.resources.C
import io.github.kakaocup.compose.node.element.ComposeScreen

class MainScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(C.Tag.main_screen_container) }) {}
