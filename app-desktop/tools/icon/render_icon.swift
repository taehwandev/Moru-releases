// Draws Moru's app icon: a taegeuk in cream and charcoal on a charcoal tile, an amber dot in the dark half.
//
//   swift render_icon.swift /tmp/moru-icon && iconutil -c icns /tmp/moru-icon/icon.iconset -o /tmp/moru-icon/icon.icns
//
// Writes icon.png (installed app), icon-dev.png (the same with an amber rim, for `gradlew run`) and icon.iconset/.
// Copy icon.png, icon-dev.png and icon.icns into src/desktopMain/resources, and icon.png over
// src/commonMain/resources/moru_logo.png. Geometry is in a 100-unit square, the tile on Apple's 82-unit grid.
import AppKit

func color(_ hex: Int) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: 1)
}

let charcoal = 0x1F2026, darkHalf = 0x33343B, cream = 0xF5F2ED, amber = 0xF6B93B

/// The icon at [size] px; [rim] draws the development build's amber outline.
func icon(_ size: Int, rim: Bool) -> Data {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size, bitsPerSample: 8, samplesPerPixel: 4,
                               hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    let scale = NSAffineTransform(); scale.scale(by: CGFloat(size) / 100); scale.concat()

    let tile = NSBezierPath(roundedRect: NSRect(x: 9, y: 9, width: 82, height: 82), xRadius: 18.5, yRadius: 18.5)
    color(charcoal).setFill(); tile.fill()
    if rim {
        let ring = NSBezierPath(roundedRect: NSRect(x: 11, y: 11, width: 78, height: 78), xRadius: 16.5, yRadius: 16.5)
        ring.lineWidth = 3; color(amber).setStroke(); ring.stroke()
    }
    color(darkHalf).setFill()
    NSBezierPath(ovalIn: NSRect(x: 21, y: 21, width: 58, height: 58)).fill()

    // The cream half, y up, as the earlier Tao icon had it: the right half of the circle, its head bulging out to the
    // left at the top and a bite taken in from the right at the bottom, where the amber dot sits.
    let half = NSBezierPath()
    half.move(to: NSPoint(x: 50, y: 21))
    half.appendArc(withCenter: NSPoint(x: 50, y: 50), radius: 29, startAngle: -90, endAngle: 90, clockwise: false)
    half.appendArc(withCenter: NSPoint(x: 50, y: 64.5), radius: 14.5, startAngle: 90, endAngle: -90, clockwise: false)
    half.appendArc(withCenter: NSPoint(x: 50, y: 35.5), radius: 14.5, startAngle: 90, endAngle: -90, clockwise: true)
    half.close()
    color(cream).setFill(); half.fill()

    color(amber).setFill()
    NSBezierPath(ovalIn: NSRect(x: 45.5, y: 31, width: 9, height: 9)).fill()
    NSGraphicsContext.restoreGraphicsState()
    return rep.representation(using: .png, properties: [:])!
}

let out = URL(fileURLWithPath: CommandLine.arguments[1])
let set = out.appendingPathComponent("icon.iconset")
try FileManager.default.createDirectory(at: set, withIntermediateDirectories: true)
try icon(512, rim: false).write(to: out.appendingPathComponent("icon.png"))
try icon(512, rim: true).write(to: out.appendingPathComponent("icon-dev.png"))
for points in [16, 32, 128, 256, 512] {
    try icon(points, rim: false).write(to: set.appendingPathComponent("icon_\(points)x\(points).png"))
    try icon(points * 2, rim: false).write(to: set.appendingPathComponent("icon_\(points)x\(points)@2x.png"))
}
print("wrote the icon into \(out.path)")
