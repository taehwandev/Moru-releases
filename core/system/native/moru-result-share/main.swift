// A user-controlled AppKit sharing chooser for one local result file. No service or recipient is preselected.
// stdout says "shown" only after presenting the picker; it is not a delivery receipt.
import AppKit

final class ResultShare: NSObject, NSWindowDelegate, NSSharingServicePickerDelegate, NSSharingServiceDelegate {
    private let file: URL
    private let panel: NSPanel
    private let shareButton: NSButton
    private let picker: NSSharingServicePicker

    init(file: URL) {
        self.file = file
        panel = NSPanel(
            contentRect: NSRect(x: 0, y: 0, width: 400, height: 116),
            styleMask: [.titled, .closable], backing: .buffered, defer: false
        )
        shareButton = NSButton(title: "Share…", target: nil, action: nil)
        picker = NSSharingServicePicker(items: [file])
        super.init()
        panel.title = "Share Result"
        panel.isReleasedWhenClosed = false
        panel.hidesOnDeactivate = false
        panel.delegate = self
        picker.delegate = self

        let name = NSTextField(labelWithString: file.lastPathComponent)
        name.frame = NSRect(x: 20, y: 76, width: 360, height: 20)
        name.lineBreakMode = .byTruncatingMiddle
        panel.contentView?.addSubview(name)
        let detail = NSTextField(labelWithString: "Choose a service to share this file.")
        detail.frame = NSRect(x: 20, y: 50, width: 360, height: 18)
        detail.textColor = .secondaryLabelColor
        detail.font = .systemFont(ofSize: NSFont.smallSystemFontSize)
        panel.contentView?.addSubview(detail)
        shareButton.frame = NSRect(x: 290, y: 12, width: 90, height: 28)
        shareButton.bezelStyle = .rounded
        shareButton.target = self
        shareButton.action = #selector(showPicker)
        panel.contentView?.addSubview(shareButton)
        let cancel = NSButton(title: "Cancel", target: self, action: #selector(cancelShare))
        cancel.frame = NSRect(x: 192, y: 12, width: 90, height: 28)
        cancel.bezelStyle = .rounded
        cancel.keyEquivalent = "\u{1b}"
        panel.contentView?.addSubview(cancel)
    }

    func present() {
        panel.center()
        panel.makeKeyAndOrderFront(nil)
        NSApplication.shared.activate(ignoringOtherApps: true)
        showPicker()
        FileHandle.standardOutput.write(Data("shown\n".utf8))
    }

    @objc private func showPicker() {
        picker.show(relativeTo: shareButton.bounds, of: shareButton, preferredEdge: .minY)
    }

    @objc private func cancelShare() { NSApplication.shared.terminate(nil) }

    func windowShouldClose(_ sender: NSWindow) -> Bool {
        cancelShare()
        return true
    }

    func sharingServicePicker(_ sharingServicePicker: NSSharingServicePicker, didChoose service: NSSharingService?) {
        guard service != nil else {
            cancelShare()
            return
        }
        shareButton.isEnabled = false
    }

    func sharingServicePicker(_ sharingServicePicker: NSSharingServicePicker, delegateFor service: NSSharingService) -> NSSharingServiceDelegate? {
        self
    }

    func sharingService(_ sharingService: NSSharingService, sourceWindowForShareItems items: [Any], sharingContentScope: UnsafeMutablePointer<NSSharingService.SharingContentScope>) -> NSWindow? {
        sharingContentScope.pointee = .item
        return panel
    }

    func sharingService(_ sharingService: NSSharingService, didShareItems items: [Any]) { cancelShare() }

    func sharingService(_ sharingService: NSSharingService, didFailToShareItems items: [Any], error: Error) { cancelShare() }
}

guard CommandLine.arguments.count == 2 else { exit(1) }
let file = URL(fileURLWithPath: CommandLine.arguments[1])
let app = NSApplication.shared
app.setActivationPolicy(.accessory)
guard FileManager.default.isReadableFile(atPath: file.path),
      !NSSharingService.sharingServices(forItems: [file]).isEmpty else { exit(1) }
let sharing = ResultShare(file: file)
DispatchQueue.main.async { sharing.present() }
app.run()
