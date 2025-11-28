package com.neptune.neptune.ui.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.NepTuneTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeptuneTopBar(
    title: String,
    goBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit) = {},
    divider: Boolean = true
) {
  Column(modifier = modifier.padding(vertical = 8.dp)) {
    CenterAlignedTopAppBar(
        title = {
          Text(
              text = title,
              style =
                  TextStyle(
                      fontSize = 45.sp,
                      fontFamily = FontFamily(Font(R.font.markazi_text)),
                      fontWeight = FontWeight(149),
                      color = NepTuneTheme.colors.onBackground,
                  ),
              modifier = Modifier.padding(25.dp),
              textAlign = TextAlign.Center)
        },
        navigationIcon = {
          IconButton(onClick = goBack, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Go Back",
                tint = NepTuneTheme.colors.onBackground)
          }
        },
        actions = actions,
        colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = NepTuneTheme.colors.background))
    if (divider) {
      HorizontalDivider(
          modifier = Modifier.fillMaxWidth(),
          thickness = 0.75.dp,
          color = NepTuneTheme.colors.onBackground)
    }
  }
}
