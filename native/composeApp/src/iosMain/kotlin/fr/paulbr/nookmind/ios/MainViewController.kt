package fr.paulbr.nookmind.ios

import androidx.compose.ui.window.ComposeUIViewController
import fr.paulbr.nookmind.App
import platform.UIKit.UIViewController

/** The view controller SwiftUI hosts: the whole shared app. Called from `ComposeView.swift` as `MainViewControllerKt.MainViewController()`. */
fun MainViewController(): UIViewController = ComposeUIViewController { App(IosApp.container) }
