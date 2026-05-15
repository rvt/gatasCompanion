import UIKit
import SwiftUI
import ComposeApp // the Kotlin framework name

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ZStack {
            ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler

            if #available(iOS 16.1, *) {
                LiveActivityBootstrapView()
            }
        }
    }
}
