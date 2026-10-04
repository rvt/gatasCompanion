import ActivityKit
import Foundation
import SwiftUI
import UIKit
import ComposeApp
import os

/// Keeps the Live Activity synchronized with the in-process bridge state.
///
/// A Live Activity is a status surface only. It does not grant background
/// execution time and must never be used as a substitute for iOS Bluetooth
/// restoration or ordinary application lifecycle handling.
@available(iOS 16.2, *)
@MainActor
final class GatasLiveActivityManager: ObservableObject {
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "GATAS", category: "LiveActivity")
    private var activity: Activity<GatasPropellerLiveActivityAttributes>?
    private var loopTask: Task<Void, Never>?

    func start() {
        guard loopTask == nil else { return }
        guard UIDevice.current.userInterfaceIdiom == .phone else { return }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        // A SwiftUI root view can be recreated without terminating an existing
        // Live Activity. Reuse the system-owned instance to avoid duplicate
        // activities and to keep updates connected to the visible island.
        let existingActivities = Activity<GatasPropellerLiveActivityAttributes>.activities
        activity = existingActivities.first { $0.activityState == .active }
        endDuplicateActivities(existingActivities.filter { $0.id != activity?.id })
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

        if let activity, activity.activityState != .active {
            logger.notice("Discarding a non-active Live Activity before requesting a replacement")
            self.activity = nil
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
            logger.error("Unable to start Live Activity: \(error.localizedDescription, privacy: .public)")
        }
    }

    @available(iOS 16.2, *)
    private func updateActivity(rotationDegrees: Int) async {
        guard let activity, activity.activityState == .active else {
            logger.notice("Skipped Live Activity update because the activity is no longer active")
            self.activity = nil
            return
        }

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

    private func endDuplicateActivities(
        _ duplicates: [Activity<GatasPropellerLiveActivityAttributes>]
    ) {
        guard !duplicates.isEmpty else { return }

        Task { [logger] in
            for duplicate in duplicates {
                let state = GatasPropellerLiveActivityAttributes.ContentState(
                    rotationDegrees: 0,
                    isRunning: false
                )
                await duplicate.end(
                    ActivityContent(state: state, staleDate: nil),
                    dismissalPolicy: .immediate
                )
                logger.notice("Ended a duplicate or stale Live Activity")
            }
        }
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
