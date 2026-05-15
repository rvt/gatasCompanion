import ActivityKit
import Foundation
import SwiftUI
import UIKit
import ComposeApp

@available(iOS 16.2, *)
@MainActor
final class GatasLiveActivityManager: ObservableObject {
    private var activity: Activity<GatasPropellerLiveActivityAttributes>?
    private var loopTask: Task<Void, Never>?

    func start() {
        guard loopTask == nil else { return }
        guard UIDevice.current.userInterfaceIdiom == .phone else { return }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        loopTask = Task { [weak self] in
            await self?.runLoop()
        }
    }

    func stop() {
        loopTask?.cancel()
        loopTask = nil
        Task { [weak self] in
            await self?.endActivity()
        }
    }

    private func runLoop() async {
        while !Task.isCancelled {
            await syncActivity()
            try? await Task.sleep(nanoseconds: 1_000_000_000)
        }
    }

    private func syncActivity() async {
        let bridge = GatasLiveActivityBridge.shared

        guard bridge.isBridgeRunning() else {
            await endActivity()
            return
        }

        if activity == nil {
            await startActivity(rotationDegrees: Int(bridge.rotationDegrees()))
            return
        }

        if bridge.consumeRotationTick() {
            await updateActivity(rotationDegrees: Int(bridge.rotationDegrees()))
        }
    }

    @available(iOS 16.2, *)
    private func startActivity(rotationDegrees: Int) async {
        let contentState = GatasPropellerLiveActivityAttributes.ContentState(
            rotationDegrees: rotationDegrees,
            isRunning: true
        )
        let content = ActivityContent(state: contentState, staleDate: nil)

        do {
            activity = try Activity.request(
                attributes: GatasPropellerLiveActivityAttributes(title: "GATAS Bridge"),
                content: content,
                pushType: nil
            )
        } catch {
            activity = nil
        }
    }

    @available(iOS 16.2, *)
    private func updateActivity(rotationDegrees: Int) async {
        guard let activity else { return }

        let contentState = GatasPropellerLiveActivityAttributes.ContentState(
            rotationDegrees: rotationDegrees,
            isRunning: true
        )
        let content = ActivityContent(state: contentState, staleDate: nil)

        await activity.update(content)
    }

    @available(iOS 16.2, *)
    private func endActivity() async {
        guard let activity else { return }
        let contentState = GatasPropellerLiveActivityAttributes.ContentState(
            rotationDegrees: Int(GatasLiveActivityBridge.shared.rotationDegrees()),
            isRunning: false
        )
        let content = ActivityContent(state: contentState, staleDate: nil)

        await activity.end(content, dismissalPolicy: .default)
        self.activity = nil
    }
}

@available(iOS 16.2, *)
struct LiveActivityBootstrapView: View {
    @StateObject private var manager = GatasLiveActivityManager()

    var body: some View {
        Color.clear
            .task {
                manager.start()
            }
            .onDisappear {
                manager.stop()
            }
    }
}
