package com.neptune.neptune.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.Sample
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.main.SampleCard
import com.neptune.neptune.ui.navigation.BottomNavigationMenu
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.projectlist.SearchBar
import com.neptune.neptune.ui.theme.NepTuneTheme
import kotlinx.coroutines.delay
/*
Search Screen Composable
Includes: The search bar and the list of samples matching the search query
Uses: SearchViewModel to get the list of samples
Clicking on the profile picture of a sample navigates to the profile screen of the poster
Clicking on like puts it in red
 */
object SearchScreenTestTags {
    const val SEARCH_SCREEN = "searchScreen"
    const val SEARCH_BAR = "searchBar"

}
class SearchScreenTestTagsPerSampleCard(idInColumn: Int = 0) : BaseSampleTestTags {
    override val prefix = "SearchScreen"

    //Sample Cards
    override val SAMPLE_CARD = "${tag("sampleCard")}_$idInColumn"
    override val SAMPLE_PROFILE_ICON = "${tag("sampleProfileIcon")}_$idInColumn"
    override val SAMPLE_USERNAME = "${tag("sampleUsername")}_$idInColumn"
    override val SAMPLE_NAME = "${tag("sampleName")}_$idInColumn"
    override val SAMPLE_DURATION = "${tag("sampleDuration")}_$idInColumn"
    override val SAMPLE_TAGS = "${tag("sampleTags")}_$idInColumn"

    override val SAMPLE_LIKES = "${tag("sampleLikes")}_$idInColumn"
    override val SAMPLE_COMMENTS = "${tag("sampleComments")}_$idInColumn"
    override val SAMPLE_DOWNLOADS = "${tag("sampleDownloads")}_$idInColumn"

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    onProfilePicClick : () -> Unit = {},
    onSampleClick : () -> Unit = {},
    onDownloadClick : () -> Unit = {},
    onLikeClick : () -> Unit = {},
    onCommentClick : () -> Unit = {},
) {
    val samples by searchViewModel.samples.collectAsState()
    var searchText by remember{ mutableStateOf("") }
    LaunchedEffect(searchText) {
        delay(300L) // debounce time
        searchViewModel.search(searchText)
    }
    val samplesStr = "Samples"
    Scaffold(
        containerColor = NepTuneTheme.colors.background,
        modifier = Modifier.testTag(SearchScreenTestTags.SEARCH_SCREEN),
        topBar = {
            SearchBar(searchText, {searchText = it},
                SearchScreenTestTags.SEARCH_BAR, samplesStr)

        },
        content = { pd ->
            ScrollableColumnOfSamples(
                samples = samples,
                onProfilePicClick = onProfilePicClick,
                 onDownloadClick = onDownloadClick,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick,
                modifier = Modifier.padding(pd)
            )
        }
    )
}

@Composable
fun ScrollableColumnOfSamples(
    samples: List<Sample>,
    onProfilePicClick : () -> Unit = {},
    onDownloadClick : () -> Unit = {},
    onLikeClick : () -> Unit = {},
    onCommentClick : () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NepTuneTheme.colors.listBackground),
        horizontalAlignment = Alignment.CenterHorizontally

    )
     {
        val width = 300
        itemsIndexed(samples) { index, sample ->
            //change height and width if necessary
            val testTags = SearchScreenTestTagsPerSampleCard(idInColumn = index)
            SampleCard(
                sample = sample,
                width = width,
                onProfileClick = onProfilePicClick,
                onDownloadClick = onDownloadClick,
                onCommentClick = onCommentClick,
                testTags = testTags
            )
        }
    }
}