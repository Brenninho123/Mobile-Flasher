import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    let accessing = url.startAccessingSecurityScopedResource()
                    IncomingFilesIosKt.openIncomingFile(path: url.path)
                    if accessing {
                        url.stopAccessingSecurityScopedResource()
                    }
                }
        }
    }
}
