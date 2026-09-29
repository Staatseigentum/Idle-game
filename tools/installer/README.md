# Windows setup

`Embercrown-Setup.exe` is an Embercrown-branded WiX Burn bundle around the
existing `:launcher:packageMsi` output. The MSI remains responsible for files,
shortcuts, the bundled Java runtime, upgrades and uninstallation. The launcher
still downloads the current platform game JAR on startup; this setup does not
replace the in-app update path or touch saved games.

Build on Windows:

```powershell
.\gradlew.bat :launcher:packageWindowsSetup
```

The output is `build/installer/Embercrown-Setup.exe`. Gradle first builds the
internal MSI and generates the game icon, then invokes `build.ps1` to wrap the
MSI. The packaging tasks provide WiX 3 in `build/wix311`; the script also finds
it via `WIX`, `PATH`, or a standard Program Files installation. The setup uses
the tracked pixel-art cover from `tools/cover`, the generated game icon, and the pixel-art title bar
assets in this folder. `node tools/installer/titlebar.js` regenerates the title
bar and its three-state close/minimize buttons.

The interface is editable in `EmbercrownTheme.xml` and its English strings in
`EmbercrownTheme.wxl`. It uses a borderless popup window with a custom draggable
Embercrown header and WiX's built-in close and minimize controls. WixStdBA still
uses a native Windows message box for the cancel confirmation, but its text is
localized in the WXL file. The Burn bundle definition is `EmbercrownSetup.wxs`.
Never change its `UpgradeCode` after releasing the first setup; it is separate
from the MSI's long-standing upgrade UUID.

The public EXE exposes an Options page for choosing the installation folder.
Its `InstallFolder` variable is passed to jpackage's MSI `INSTALLDIR` property;
the default remains the per-user LocalAppData/Embercrown directory. The MSI is
not a separate public download. Test new install, upgrade from the existing MSI,
repair, uninstall, custom paths and preservation of save data in a clean VM
before releasing. The EXE is not yet signed; code signing should be added
before treating SmartScreen prompts as resolved.
