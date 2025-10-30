package com.neptune.neptune.utils // or your testutil package

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/*
    JUnit Test Rule to set the Main dispatcher to a TestDispatcher for unit tests.
    This allows tests to run coroutines that use Dispatchers.Main without Android dependencies.
    Written with help from ChatGPT.
*/

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher(), TestRule {

  override fun starting(description: Description) {
    // Replaces Dispatchers.Main with our test dispatcher
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    // Reset Main after test to avoid leaking
    Dispatchers.resetMain()
  }
}
