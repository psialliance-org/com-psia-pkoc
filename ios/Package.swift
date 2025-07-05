// swift-tools-version: 5.6
import PackageDescription


let package = Package(
    name: "PSIAExperienceApp",
    platforms: [
        .iOS(.v14)
    ],
    products: [
        // Products define the executables and libraries a package produces, and make them visible to other packages.
        .library(
            name: "PSIAExperienceApp",
            targets: ["PSIAExperienceApp"])
    ],
    dependencies: [
        .package(url: "https://github.com/attaswift/BigInt.git", from: "5.4.0"),
        .package(url: "https://github.com/krzyzanowskim/CryptoSwift.git", from: "1.8.3"),
        .package(url: "https://github.com/BastiaanJansen/toast-swift", from: "2.1.2")    ],
    targets: [
        // Targets are the basic building blocks of a package. A target can define a module or a test suite.
        // Targets can depend on other targets in this package, and on products in packages this package depends on.
        .target(
            name: "PSIAExperienceApp",
            dependencies: [
                .product(name: "BigInt", package: "BigInt"),
                .product(name: "CryptoSwift", package: "CryptoSwift"),
                .product(name: "Toast", package: "toast-swift")
            ],
            exclude: [],
            resources: [
                .copy("PSIAExperienceApp.entitlements"),
                .copy("Assets.xcassets")
            ]
        )
    ],
    swiftLanguageVersions: [.v5]
)
